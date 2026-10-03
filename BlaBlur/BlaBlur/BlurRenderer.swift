import UIKit
import CoreImage

enum BlurFailure: LocalizedError {
    case empty, tooLong, rendering, photosDenied
    var errorDescription: String? {
        switch self {
        case .empty: return "클립보드에 텍스트가 없습니다. 글을 복사하거나 아래 버튼으로 붙여넣어 주세요."
        case .tooLong: return "글이 너무 깁니다. 내용을 나누어 복사해 주세요. (최대 10,000자)"
        case .rendering: return "이미지를 만들지 못했습니다. 다시 시도해 주세요."
        case .photosDenied: return "사진 추가 권한이 필요합니다. 설정에서 사진 접근을 허용한 뒤 ‘사진에 저장’을 눌러 주세요."
        }
    }
}

enum BlurRenderer {
    static let phrases = [
        "오늘의 나는 충분히 몰입했는가?",
        "오늘 좋았거나 안좋았던 거 하나 돌아보기:",
        "내일의 내게 들려주고 싶은 말은 무엇인가?"
    ]

    static func ranges(in text: String) throws -> [NSRange] {
        let pattern = phrases.sorted { $0.count > $1.count }
            .map(NSRegularExpression.escapedPattern(for:)).joined(separator: "|")
        return try NSRegularExpression(pattern: pattern).matches(
            in: text, range: NSRange(text.startIndex..., in: text)
        ).map(\.range)
    }

    static func dateTitle(now: Date, calendar: Calendar = .current) -> String {
        let yesterday = calendar.date(byAdding: .day, value: -1, to: now) ?? now
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "yyyy. M. d."
        return formatter.string(from: yesterday)
    }

    // Both passes retain identical fonts and geometry. Only their ink changes.
    @MainActor
    static func render(_ text: String, now: Date = Date()) throws -> UIImage {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw BlurFailure.empty }
        guard text.count <= 10_000 else { throw BlurFailure.tooLong }
        let matches = try ranges(in: text)
        let paragraph = NSMutableParagraphStyle()
        paragraph.lineSpacing = 7
        paragraph.lineBreakMode = .byCharWrapping
        let base = NSMutableAttributedString(string: text, attributes: [
            .font: UIFont.systemFont(ofSize: 16), .foregroundColor: UIColor.black,
            .paragraphStyle: paragraph
        ])
        for range in matches { base.addAttribute(.font, value: UIFont.boldSystemFont(ofSize: 16), range: range) }

        func layout(_ string: NSAttributedString) -> (NSTextStorage, NSLayoutManager, NSTextContainer) {
            let storage = NSTextStorage(attributedString: string)
            let manager = NSLayoutManager()
            let container = NSTextContainer(size: CGSize(width: 342, height: .greatestFiniteMagnitude))
            container.lineFragmentPadding = 0
            manager.addTextContainer(container)
            storage.addLayoutManager(manager)
            manager.ensureLayout(for: container)
            return (storage, manager, container)
        }
        let measured = layout(base)
        let height = max(480, ceil(measured.1.usedRect(for: measured.2).height) + 120)
        guard height <= 6000 else { throw BlurFailure.tooLong }
        let size = CGSize(width: 390, height: height)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 2
        format.opaque = false
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        var privateRanges: [NSRange] = []
        var cursor = 0
        for range in matches {
            if range.location > cursor { privateRanges.append(NSRange(location: cursor, length: range.location - cursor)) }
            cursor = NSMaxRange(range)
        }
        if cursor < base.length { privateRanges.append(NSRange(location: cursor, length: base.length - cursor)) }
        // Draw only the selected glyphs, so even color emoji cannot leak into the clear pass.
        func drawText(_ ranges: [NSRange]) {
            let (storage, manager, _) = layout(base)
            withExtendedLifetime(storage) {
                for range in ranges {
                    let glyphs = manager.glyphRange(forCharacterRange: range, actualCharacterRange: nil)
                    manager.drawGlyphs(forGlyphRange: glyphs, at: CGPoint(x: 24, y: 86))
                }
            }
        }
        let privateLayer = renderer.image { _ in drawText(privateRanges) }
        guard let input = CIImage(image: privateLayer),
              let filter = CIFilter(name: "CIGaussianBlur") else { throw BlurFailure.rendering }
        filter.setValue(input, forKey: kCIInputImageKey)
        filter.setValue(12, forKey: kCIInputRadiusKey)
        guard let output = filter.outputImage,
              let cgImage = CIContext().createCGImage(output, from: input.extent) else { throw BlurFailure.rendering }
        let blurred = UIImage(cgImage: cgImage, scale: 2, orientation: .up)
        return renderer.image { context in
            UIColor.white.setFill()
            context.fill(CGRect(origin: .zero, size: size))
            (dateTitle(now: now) as NSString).draw(at: CGPoint(x: 24, y: 25), withAttributes: [
                .font: UIFont.boldSystemFont(ofSize: 20), .foregroundColor: UIColor.black
            ])
            UIColor(white: 0.9, alpha: 1).setFill()
            context.fill(CGRect(x: 24, y: 62, width: 342, height: 1))
            blurred.draw(at: .zero)
            drawText(matches)
        }
    }
}
