import UIKit

/// Displays a table of all workouts (date + duration). No detail on tap yet.
class WorkoutHistoryViewController: UITableViewController {

    private var workouts: [Workout] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Workout History"
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "WorkoutCell")
        fetchWorkouts()
    }

    private func fetchWorkouts() {
        APIClient.get("/workouts", responseType: [Workout].self) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let workouts):
                    self?.workouts = workouts
                    self?.tableView.reloadData()
                case .failure(let error):
                    self?.showError(error)
                }
            }
        }
    }

    private func showError(_ error: Error) {
        let alert = UIAlertController(title: "Error",
                                      message: error.localizedDescription,
                                      preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }

    // MARK: - UITableViewDataSource

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return workouts.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "WorkoutCell", for: indexPath)
        let workout = workouts[indexPath.row]
        cell.textLabel?.text = workout.date
        let duration = workout.durationMin.map { "\($0) min" } ?? ""
        cell.detailTextLabel?.text = [duration, workout.notes ?? ""]
            .filter { !$0.isEmpty }
            .joined(separator: " — ")
        return cell
    }

    // MARK: - UITableViewDelegate

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        // TODO: Navigate to WorkoutDetailViewController
    }
}
