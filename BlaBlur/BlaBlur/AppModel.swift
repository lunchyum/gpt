import SwiftUI
import Photos
import CryptoKit

@MainActor
final class AppModel: ObservableObject {
    @Published var image: UIImage?
    @Published var busy = false
    @Published var message = "복사한 글을 기다리고 있어요."
    @Published var errorMessage: String?
    @Published var showWebsite = false
    private var attemptedChange: Int?
    private var currentKey: String?
    private var started = false

    func activate() async {
        let pasteboard = UIPasteboard.general
        guard !busy, !showWebsite else { return }
        guard !started || attemptedChange != pasteboard.changeCount else { return }
        started = true
        attemptedChange = pasteboard.changeCount
        guard let text = pasteboard.string else {
            message = "텍스트를 복사하거나 ‘붙여넣기’를 눌러 주세요."
            return
        }
        await process(text)
    }

    func process(_ text: String) async {
        guard !busy else { return }
        busy = true
        defer { busy = false }
        errorMessage = nil
        image = nil
        currentKey = nil
        do {
            message = "블러 이미지를 만드는 중…"
            let day = BlurRenderer.dateTitle(now: Date())
            let key = SHA256.hash(data: Data((day + "\n" + text).utf8))
                .map { String(format: "%02x", $0) }.joined()
            let result = try BlurRenderer.render(text)
            image = result
            currentKey = key
            if savedKeys.contains(key) {
                message = "오늘 이미 저장한 내용이에요."
                showWebsite = true
                return
            }
            try await save(result)
            remember(key)
            message = "사진에 저장했어요."
            showWebsite = true
        } catch {
            message = "아래 안내를 확인해 주세요."
            errorMessage = error.localizedDescription
        }
    }

    func saveAgain() async {
        guard !busy, let image else { return }
        busy = true
        defer { busy = false }
        do {
            try await save(image)
            if let currentKey { remember(currentKey) }
            errorMessage = nil
            message = "사진에 저장했어요."
            showWebsite = true
        } catch { errorMessage = error.localizedDescription }
    }

    private var savedKeys: [String] { UserDefaults.standard.stringArray(forKey: "savedImageKeys.v1") ?? [] }
    private func remember(_ key: String) {
        var keys = savedKeys.filter { $0 != key }
        keys.append(key)
        UserDefaults.standard.set(Array(keys.suffix(120)), forKey: "savedImageKeys.v1")
    }
    private func save(_ image: UIImage) async throws {
        message = "사진에 저장하는 중…"
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else { throw BlurFailure.photosDenied }
        guard let data = image.pngData() else { throw BlurFailure.rendering }
        try await PHPhotoLibrary.shared().performChanges {
            let request = PHAssetCreationRequest.forAsset()
            let options = PHAssetResourceCreationOptions()
            options.uniformTypeIdentifier = "public.png"
            request.addResource(with: .photo, data: data, options: options)
        }
    }
}
