import SwiftUI
import SafariServices

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model = AppModel()
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("질문은 선명하게,\n나의 이야기는 흐릿하게.")
                            .font(.largeTitle.bold())
                        Text("복사한 글을 블러 처리하고 사진에 저장해요.")
                            .foregroundStyle(.secondary)
                    }
                    Label(model.message, systemImage: model.busy ? "hourglass" : "sparkles")
                        .font(.callout)
                    if model.busy { ProgressView() }
                    if let error = model.errorMessage {
                        Text(error).font(.callout).foregroundStyle(.red)
                        Button("앱 설정 열기") {
                            if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                        }
                    }
                    HStack {
                        PasteButton(payloadType: String.self) { values in
                            guard let text = values.first else { return }
                            Task { await model.process(text) }
                        }.labelStyle(.titleAndIcon)
                        if model.image != nil {
                            Button {
                                Task { await model.saveAgain() }
                            } label: {
                                Label("사진에 저장", systemImage: "square.and.arrow.down")
                            }.buttonStyle(.bordered)
                        }
                    }.disabled(model.busy)
                    if let image = model.image {
                        Image(uiImage: image).resizable().scaledToFit()
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(.black.opacity(0.06)))
                            .accessibilityLabel("세 질문을 제외한 내용이 블러 처리된 저장 이미지")
                    }
                    Button { model.showWebsite = true } label: {
                        HStack { Text("Odyssey 열기"); Spacer(); Image(systemName: "arrow.up.right") }
                            .padding()
                    }.buttonStyle(.borderedProminent).disabled(model.busy)
                    Text("세 문구가 정확히 일치하는 부분만 선명하게 남아요. 같은 날짜의 같은 내용은 자동으로 중복 저장하지 않아요.")
                        .font(.footnote).foregroundStyle(.secondary)
                }.padding(24)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .navigationTitle("블라블러")
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(.indigo)
        .task { await model.activate() }
        .onChange(of: scenePhase) { phase in
            if phase == .active { Task { await model.activate() } }
        }
        .sheet(isPresented: $model.showWebsite) {
            OdysseyBrowser().ignoresSafeArea()
        }
    }
}

// In-app Safari supplies secure navigation, login, downloads, and a Done button.
// Clipboard contents and images are never injected into the website.
struct OdysseyBrowser: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: URL(string: "https://odyssey.zstrit.com")!)
        controller.dismissButtonStyle = .done
        return controller
    }
    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {}
}
