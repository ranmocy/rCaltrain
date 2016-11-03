//
//  ResultTableView.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 10/22/14.
//  Copyright (c) 2014 Ranmocy. All rights reserved.
//

import UIKit

class ResultTableView:UITableView, UITableViewDataSource {

    var results:[Result] = []

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return results.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let resultCell = tableView.dequeueReusableCell(withIdentifier: "resultCell") as? ResultTableViewCell {
            resultCell.updateData(results[(indexPath as NSIndexPath).row])
            return resultCell
        } else {
            fatalError("No resultCell in ResultTableView!")
        }
    }

}
