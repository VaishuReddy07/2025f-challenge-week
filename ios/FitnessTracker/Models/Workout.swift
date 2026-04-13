import Foundation

struct Workout: Codable, Identifiable {
    let id: Int
    let date: String
    let durationMin: Int?
    let notes: String?

    enum CodingKeys: String, CodingKey {
        case id
        case date
        case durationMin = "duration_min"
        case notes
    }
}
