//
//  MainViewController.swift
//  rCaltrain
//
//  Created by Ranmocy on 9/30/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import UIKit

class MainViewController: UIViewController {

    var departurePlaceholder: String = "Departure"
    var arrivalPlaceholder: String = "Arrival"

    @IBOutlet var departureButton: UIButton!
    @IBOutlet var arrivalButton: UIButton!
    @IBOutlet var whenButton: UISegmentedControl!
    @IBOutlet var reverseButton: UIButton!
    @IBOutlet var resultsTableView: ResultTableView!

    @IBAction func unwindFromModalViewController(_ segue: UIStoryboardSegue) {
        if let _ = segue.identifier {
            updateResults()
        } else {
            fatalError("Unexpected segue without identifier!")
        }
    }

    @IBAction func reversePressed(_ sender: UIButton) {
        let departureTitle = departureButton.currentTitle
        let arrivalTitle = arrivalButton.currentTitle

        if arrivalTitle == arrivalPlaceholder {
            departureButton.setTitle(departurePlaceholder, for: UIControlState())
        } else {
            departureButton.setTitle(arrivalTitle, for: UIControlState())
        }

        if departureTitle == departurePlaceholder {
            arrivalButton.setTitle(arrivalPlaceholder, for: UIControlState())
        } else {
            arrivalButton.setTitle(departureTitle, for: UIControlState())
        }

        updateResults()
    }

    @IBAction func whenChanged(_ sender: UISegmentedControl) {
        updateResults()
    }

    func savePreference(_ from: String, to: String, when: Int) {
        let pref = UserDefaults.standard
        pref.set(from, forKey: "from")
        pref.set(to, forKey: "to")
        pref.set(when, forKey: "when")
        pref.synchronize()
    }

    func loadPreference() {
        let pref = UserDefaults.standard

        if let from = pref.string(forKey: "from") {
            departureButton.setTitle(from, for: UIControlState())
        }

        if let to = pref.string(forKey: "to") {
            arrivalButton.setTitle(to, for: UIControlState())
        }

        let when = pref.integer(forKey: "when")
        let length = whenButton.numberOfSegments
        if (0 <= when && when < length) {
            whenButton.selectedSegmentIndex = when
        }
    }

    override func viewDidLoad() {
        // load placeholder
        departureButton.setTitle(departurePlaceholder, for: UIControlState())
        arrivalButton.setTitle(arrivalPlaceholder, for: UIControlState())

        // setups
        resultsTableView.dataSource = resultsTableView
        super.viewDidLoad()

        // init update
        loadPreference()
        updateResults()
    }

    // Get inputs value. If some input is missing, return nil
    // Return: ([departure_stations], [arrival_stations], category, isNow)?
    func getInputs() -> ([Station], [Station], String)? {
        var departureStations: [Station]
        var arrivalStations: [Station]
        var category: String

        // get departure stations
        if let dName = departureButton.currentTitle {
            if let stations = Station.getStations(byName: dName) {
                departureStations = stations
            } else {
                return nil
            }
        } else {
            fatalError("departureButton's title is missing!")
        }

        // get arrival stations
        if let aName = arrivalButton.currentTitle {
            if let stations = Station.getStations(byName: aName) {
                arrivalStations = stations
            } else {
                return nil
            }
        } else {
            fatalError("arrivalButton's title is missing!")
        }

        // get service category
        if (whenButton.selectedSegmentIndex == UISegmentedControlNoSegment) {
            return nil
        } else if let name = whenButton.titleForSegment(at: whenButton.selectedSegmentIndex) {
            category = name
        } else {
            fatalError("whenButton's title is missing!")
        }

        savePreference(departureButton.currentTitle!, to: arrivalButton.currentTitle!, when: whenButton.selectedSegmentIndex)

        return (departureStations, arrivalStations, category)
    }

    func updateResults() {
        // if inputs are ready update, otherwise ignore it
        if let (departureStations, arrivalStations, category) = getInputs() {
            var results = [Result]()
            let services = Service.getAllServices().filter { s in
                return s.isValidAt(category)
            }

            for service in services {
                for (_, trip) in service.trips {
                    for dStation in departureStations {
                        for aStation in arrivalStations {
                            if let (from, to) = trip.findFrom(dStation, to: aStation) {
                                // check if it's a valid stop
                                if (category != "Now" || from.laterThanNow) {
                                    results.append(Result(departure: from, arrival: to))
                                }
                            }
                        }
                    }
                }
            }

            results.sort { $0.departureTime < $1.departureTime }

            resultsTableView.results = results
            resultsTableView.reloadData()
        }
    }

}

