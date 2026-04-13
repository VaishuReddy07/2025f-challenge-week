import Foundation

struct Exercise: Codable, Identifiable {
    let id: Int
    let name: String
    let category: String
    let description: String?
}
