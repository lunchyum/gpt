import XCTest
@testable import BlaBlur

final class BlurRendererTests: XCTestCase {
    func testRepeatedQuestionsWithEmojiAndKorean() throws {
        let phrase = BlurRenderer.phrases[0]
        let text = "👨‍👩‍👧‍👦 비밀\n\(phrase) 답\n\(phrase)"
        let ranges = try BlurRenderer.ranges(in: text)
        XCTAssertEqual(ranges.count, 2)
        for range in ranges { XCTAssertEqual((text as NSString).substring(with: range), phrase) }
    }
    func testExactMatchesOnly() throws {
        XCTAssertTrue(try BlurRenderer.ranges(in: "오늘의 나는 충분히 몰입했는가! <script>비밀</script>").isEmpty)
        XCTAssertEqual(try BlurRenderer.ranges(in: BlurRenderer.phrases.joined(separator: "\n")).count, 3)
    }
    func testYesterdayAcrossYearBoundary() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul")!
        let date = calendar.date(from: DateComponents(year: 2026, month: 1, day: 1, hour: 1))!
        XCTAssertEqual(BlurRenderer.dateTitle(now: date, calendar: calendar), "2025. 12. 31.")
    }
    @MainActor func testImageAndInputLimits() throws {
        let image = try BlurRenderer.render("오늘의 나는 충분히 몰입했는가?\n개인적인 답변 🥰")
        XCTAssertEqual(image.size.width, 390)
        XCTAssertNotNil(image.pngData())
        XCTAssertThrowsError(try BlurRenderer.render(" \n "))
        XCTAssertThrowsError(try BlurRenderer.render(String(repeating: "가", count: 10_001)))
    }
}
