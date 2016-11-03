//
//  StationViewController.swift
//  rCaltrain
//
//  Created by Ranmocy on 10/2/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import UIKit

class StationViewController: UITableViewController, UISearchResultsUpdating {

    var stationNames = [String]()
    var filteredNames = [String]()

    // search functionality
    @IBOutlet var searchBar: UISearchBar!
    var resultSearchController = UISearchController()


    // Table View
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if self.resultSearchController.isActive {
            return filteredNames.count
        } else {
            return stationNames.count
        }
    }
    
    func reusableCellName() -> String {
        fatalError("reusable cell name need to be specified by subclass!")
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: self.reusableCellName(), for: indexPath) as UITableViewCell
        var stations: [String]
        if self.resultSearchController.isActive {
            stations = filteredNames
        } else {
            stations = stationNames
        }
        cell.textLabel?.text = stations[(indexPath as NSIndexPath).row]

        return cell
    }


    // Search View
    func updateSearchResults(for searchController: UISearchController) {
        filterStations(searchController.searchBar.text!)
        self.tableView.reloadData()
    }

    func selectionIdentifier() -> String {
        fatalError("selectionIdentifier should be specified by subclass!")
    }

    func selectionCallback(_ controller: MainViewController, selectionText: String) {
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let id = segue.identifier {
            switch (id) {
            case selectionIdentifier():
                let destViewController = segue.destination as! MainViewController
                var name: String
                var table: [String]

                if (self.resultSearchController.isActive) {
                    table = filteredNames
                } else {
                    table = stationNames
                }

                if let row = (self.tableView.indexPathForSelectedRow as NSIndexPath?)?.row {
                    name = table[row]
                } else {
                    fatalError("unexpected: no row is selected in \(selectionIdentifier())")
                }

                self.resultSearchController.isActive = false

                selectionCallback(destViewController, selectionText: name)
            default:
                print(segue.identifier)
                return
            }
        }
    }


    override func viewDidLoad() {
        super.viewDidLoad()

        stationNames = Station.getNames()

        self.resultSearchController = ({
            let controller = UISearchController(searchResultsController: nil)
            controller.searchResultsUpdater = self
            controller.hidesNavigationBarDuringPresentation = true
            controller.dimsBackgroundDuringPresentation = false
            controller.searchBar.sizeToFit()

            self.tableView.tableHeaderView = controller.searchBar

            return controller
        })()
    }

    // private helper

    func filterStations(_ searchText: String) {
        if (searchText == "") {
            filteredNames = stationNames
        } else {
            let regexp = try! NSRegularExpression(pattern: searchText, options: .caseInsensitive)
            filteredNames = stationNames.filter() {
                regexp.numberOfMatches(in: $0, options: [], range: NSMakeRange(0, $0.length)) > 0
            }
        }
    }

}
