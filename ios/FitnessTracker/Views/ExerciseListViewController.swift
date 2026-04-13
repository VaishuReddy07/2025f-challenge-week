import UIKit

/// Displays a table of all exercises grouped by category.
class ExerciseListViewController: UITableViewController {

    private var exercisesByCategory: [(category: String, exercises: [Exercise])] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Exercises"
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "ExerciseCell")
        fetchExercises()
    }

    private func fetchExercises() {
        APIClient.get("/exercises", responseType: [Exercise].self) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let exercises):
                    self?.groupByCategory(exercises)
                    self?.tableView.reloadData()
                case .failure(let error):
                    self?.showError(error)
                }
            }
        }
    }

    private func groupByCategory(_ exercises: [Exercise]) {
        let grouped = Dictionary(grouping: exercises, by: { $0.category })
        exercisesByCategory = grouped
            .sorted { $0.key < $1.key }
            .map { (category: $0.key, exercises: $0.value) }
    }

    private func showError(_ error: Error) {
        let alert = UIAlertController(title: "Error",
                                      message: error.localizedDescription,
                                      preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }

    // MARK: - UITableViewDataSource

    override func numberOfSections(in tableView: UITableView) -> Int {
        return exercisesByCategory.count
    }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return exercisesByCategory[section].category
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return exercisesByCategory[section].exercises.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "ExerciseCell", for: indexPath)
        let exercise = exercisesByCategory[indexPath.section].exercises[indexPath.row]
        cell.textLabel?.text = exercise.name
        cell.detailTextLabel?.text = exercise.description
        return cell
    }
}
