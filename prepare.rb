#!/usr/bin/env ruby

def prepare_data
  require "csv"

  stops = CSV.read("gtfs/stops.txt").map! { |s| [s[0], s[2]]}
  header = stops.shift
  stops
    .delete_if { |s| /Station/.match(s.last) }
    .map! { |s| s.last.gsub!(/ Caltrain/, ''); s }
  stops.unshift(header)
  CSV.open("data/stops.csv", "wb") { |c| stops.each { |i| c << i } }

  times = CSV.read("gtfs/stop_times.txt").map! { |s| s[0..4]}
  header = times.shift
  times
    .keep_if { |s| /14OCT/.match(s[0]) }
    .map! { |s| id = s[0].split('-'); s[0] = [id[0], id[4]].join('-'); s }
  times.unshift(header)
  CSV.open("data/times.csv", "wb") { |c| times.each { |i| c << i } }

  puts "Prepared Data."
end

def enable_appcache
  require 'tempfile'
  require 'fileutils'

  path = 'index.html'
  temp_file = Tempfile.new('index.html')
  begin
    File.open(path, 'r') do |file|
      file.each_line do |line|
        if line.match("<html>")
          temp_file.puts '<html manifest="rCaltrain.appcache">'
        else
          temp_file.puts line
        end
      end
    end
    temp_file.close
    FileUtils.mv(temp_file.path, path)
  ensure
    temp_file.close
    temp_file.unlink
  end

  puts "Enabled Appcache."
end

def update_appcache
  require 'tempfile'
  require 'fileutils'

  path = 'rCaltrain.appcache'
  temp_file = Tempfile.new('rCaltrain.appcache')
  begin
    File.open(path, 'r') do |file|
      file.each_line do |line|
        if line.match(/# Updated at /)
          temp_file.puts "# Updated at #{Time.now}"
        else
          temp_file.puts line
        end
      end
    end
    temp_file.close
    FileUtils.mv(temp_file.path, path)
  ensure
    temp_file.close
    temp_file.unlink
  end

  puts "Updated Appcache."
end

def minify_files
  `uglifyjs src/default.js -o javascripts/default.min.js -c -m`
  `uglifycss src/default.css > stylesheets/default.min.css`

  puts "Minified files."
end


if __FILE__==$0
  begin
    `git checkout gh-pages`
    `git checkout master -- .`
    prepare_data
    enable_appcache
    update_appcache
    minify_files
    `git add .`
    `git commit -m 'Updated at #{Time.now}.'`
  #   `git push`
  # ensure
  #   `git checkout master`
  end
end
