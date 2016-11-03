//
//  WhenViewController.swift
//  rCaltrain
//
//  Created by Ranmocy on 10/2/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import UIKit

enum When: String {
    case fromNow = "From Now"
    case weekday = "Weekday"
    case saturday = "Saturday"
    case sunday = "Sunday"
}

class WhenViewController: UITableViewController {
    var services: [When] = [
        .fromNow,
        .weekday,
        .saturday,
        .sunday
    ]

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.services.count
    }

    func reusableCellName() -> String {
        return "whenCell"
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let possibleCell = tableView.dequeueReusableCell(withIdentifier: self.reusableCellName()) as UITableViewCell?
        assert(possibleCell != nil, "reusableCell is missing!")

        let cell = possibleCell!
        let service = self.services[(indexPath as NSIndexPath).row]
        cell.textLabel!.text = service.rawValue
        cell.detailTextLabel?.text = String(service.hashValue)

        return cell
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let id = segue.identifier {
            switch (id) {
            case "selectWhenService":
                if let _ = (self.tableView.indexPathForSelectedRow as NSIndexPath?)?.row {
//                    let name: String = self.services[row].rawValue
//                    let destViewController = segue.destinationViewController as MainViewController
                    print("change whenButton")
                } else {
                    fatalError("unexpected: no row is selected")
                }
            default:
                return
            }
        }
    }

}
