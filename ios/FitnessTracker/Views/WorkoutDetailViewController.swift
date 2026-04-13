import UIKit

/// TODO: Display the full workout detail — exercises with sets, reps, and weight.
///
/// Students should:
/// 1. Receive the workout ID from the previous screen.
/// 2. Call GET /workouts/:id to fetch the workout with its exercises.
/// 3. Display the workout info and a table of exercises.
class WorkoutDetailViewController: UIViewController {

    var workoutId: Int?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Workout Detail"
        view.backgroundColor = .systemBackground

        let label = UILabel()
        label.text = "TODO: Implement workout detail view"
        label.textColor = .secondaryLabel
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }
}
