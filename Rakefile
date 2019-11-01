class Hash
  def mapHash(&block)
    if block_given?
      self.inject({}) { |h, (k,v)| h[k] = yield(k, v); h }
    else
      raise "block is needed for map."
    end
  end
end

class File
  def self.write(filename, content, mode='')
    open(filename, "w#{mode}") { |f| f.write(content) }
  end
end

def ASSERT(condition, msg = 'Failed assertion')
  unless condition
    require 'pry'; binding.pry
    throw msg
  end
end

def FAIL(msg = 'Failed')
  require 'pry'; binding.pry
  throw msg
end

task default: :spec

desc "Download test data"
task :download_test_data do
  require 'capybara'
  require 'capybara/dsl'
  require 'webdrivers/chromedriver'
  require 'nokogiri'
  require 'json'

  Capybara.reset!
  Capybara.configure do |config|
    config.default_driver = :selenium_chrome_headless
    config.run_server = false
    config.default_max_wait_time = 0
  end

  class WebScraper
    include Capybara::DSL

    def getStyle(node, style_name)
      ASSERT(['backgroundColor', 'color', 'fontWeight'].include?(style_name.to_s))
      unless node.instance_variable_defined?(:@style)
        # serialization is expensive, so we only return what we need
        style = page.evaluate_script("\
        (function() {
          var temp_style = window.getComputedStyle(document.querySelector('#{node.css_path}'))
          return {
            backgroundColor: temp_style.backgroundColor,
            color: temp_style.color,
            fontWeight: temp_style.fontWeight,
          }
        })()")
        node.instance_variable_set(:@style, style)
      end
      style = node.instance_variable_get(:@style)
      style[style_name.to_s]
    end

    # A map from color to [class_name, display_name]
    # If class_name is nil, don't check it
    # If display_name is nil, service type is determined by the first cell in the same column
    SERVICE_TYPE_COLOR_MAPPING = {
      'rgb(240,178,161)' => ['bullet', 'Baby Bullet'], # light red
      'rgb(247,232,157)' => ['limited', 'Limited'], # yellow
      'rgba(0,0,0,0)'       => [nil, 'Local'], # transparent
      'rgb(255,255,255)' => [nil, 'Local'], # white
      'rgb(0,0,0)'       => [nil, 'SatOnly'], # black
      'rgb(189,220,155)' => [nil, nil], # green for "Timed transfers for local service"
    }
    def getServiceType(node)
      color = getStyle(node, :backgroundColor).gsub(/[[:space:]]/, '')

      ASSERT(SERVICE_TYPE_COLOR_MAPPING.include?(color), 'Unexpected cell color:' + color)
      class_name, display_name = SERVICE_TYPE_COLOR_MAPPING[color]
      ASSERT((node.text.strip.empty? or !class_name or node.classes.include?(class_name)),
             "Unexpected class name: Expect #{class_name} in #{node.classes}.")

      if display_name == nil
        parts = node.path.rpartition(/tr\[\d+\]/i)
        ASSERT(parts[1] != 'tr[1]', "Transfer can't be in the start station!")
        parts[1] = 'tr[1]'
        first_node_in_same_column = node.at_xpath(parts.join(''))
        ASSERT(first_node_in_same_column, 'Can not find first cell in the column')

        getServiceType(first_node_in_same_column)
      else
        display_name
      end
    end

    def isPmStyle(node)
      weight = getStyle(node, :fontWeight)
      case weight
      when 'normal', '400', nil
        false
      when 'bold'
        true
      else
        FAIL('Unknown font weight:' + weight)
      end
    end

    def getTime(str, service_type, is_pm_style)
      case str
      when ''
        nil
      when /\A(\d?\d):(\d\d)([ap])\Z/
        hours = $1.to_i
        minutes = $2.to_i
        is_pm = ($3 == 'p')
        if !is_pm or ((hours == 12 or hours < 3) and service_type == 'SatOnly')
          # AM. for weekend SatOnly data, some are actually am
          hours += 12 if hours == 12 # 12am to 24
          hours += 24 if hours < 3   # 1am to 25, assume no train start before 3
        else
          # PM
          hours += 12 if hours != 12 # 1pm to 13
        end
        [hours, minutes].map { |i| i.to_s.rjust(2, '0') }.join(':')
      when /\A\d?\d:\d\d\Z/
        t = str.split(':').map(&:to_i)
        if !is_pm_style or ((t[0] == 12 or t[0] < 3) and service_type == 'SatOnly')
          # AM. for weekend SatOnly data, some are actually am
          t[0] += 12 if t[0] == 12 # 12am to 24
          t[0] += 24 if t[0] < 3   # 1am to 25, assume no train start before 3
        else
          # PM
          t[0] += 12 if t[0] != 12 # 1pm to 13
        end
        t.map { |i| i.to_s.rjust(2, '0') }.join(':')
      else
        FAIL("Unknown time:" + str)
      end
    end

    def normalizeName(text)
      text.strip
        .gsub(/[[:space:]]/, 32.chr) # unify all space chars
        .gsub('So. San Francisco', 'South San Francisco')
        .gsub('SJ Diridon', 'San Jose Diridon')
    end

    def getSchedule(doc, direction)
      doc.xpath("//table[@class=\"#{direction}\"]/tbody/tr").map { |tr|
        name_cell = tr.at_xpath('th[2]')

        # skip shuttle bus
        if name_cell.classes.include?('ct-shuttle')
          ASSERT(['Shuttle Bus', 'Departs SJ Diridon', 'Arrives Tamien', 'Departs Tamien', 'Arrives SJ Diridon'].include?(name_cell.text))
          next nil
        end

        station_name = normalizeName(name_cell.at_xpath('a').text)
        # Skip shuttle stop for SJ Diridon, in favor of train's schedule
        if name_cell.classes.include?('ct-shuttle') and station_name == 'San Jose Diridon'
          next nil
        end

        {
          name: station_name,
          stop_times:
            tr.xpath('td').map { |td|
              service_type = getServiceType(td)
              is_pm_style = isPmStyle(td)
              text = td.text
                .gsub([160].pack('U*'), '')
                .gsub([8211].pack('U*'), '')
                .gsub([8212].pack('U*'), '')
                .gsub(/[\+\*\-]/, '')
                .strip

              {
                service_type: service_type,
                time: getTime(text, service_type, is_pm_style),
              }
            }
        }
      }.keep_if { |data| data }
    end

    def get()
      [
        {
          type_name: 'weekday',
          url: 'http://www.caltrain.com/schedules/weekdaytimetable.html',
        },
        {
          type_name: 'weekend',
          url: 'http://www.caltrain.com/schedules/weekend-timetable.html',
        },
      ].each { |item|
        puts "Visiting #{item[:type_name]}..."
        visit(item[:url])
        doc = Nokogiri::HTML(page.html)
        ["NB_TT", "SB_TT"].each { |direction|
          puts "Getting #{item[:type_name]}-#{direction}..."

          schedule = getSchedule(doc, direction)
          ASSERT(schedule.size >= 10, "schedule size is too small")
          sizes = schedule.map { |t| t[:stop_times].size }.uniq
          ASSERT(sizes.size == 1, 'stop_times sizes are not equal')
          ASSERT(sizes[0] > 5, 'stop_times are too few')

          File.write("test/#{item[:type_name]}_#{direction}.json", schedule.to_json)
        }
      }
    end
  end

  WebScraper.new.get()
end

desc "Run test"
task :run_test do
  require 'capybara'
  require 'capybara/dsl'
  require 'webdrivers/chromedriver'
  require 'rack'
  require 'json'

  Capybara.reset!
  Capybara.configure do |config|
    # Start a local server for serving
    config.app = Rack::File.new File.dirname __FILE__
    config.run_server = true
    config.server = :webrick
    # Config test driver
    config.default_driver = :selenium_chrome_headless
    config.default_max_wait_time = 1
  end

  class Runner
    include Capybara::DSL

    def fixTimeFormat(time_str)
      parts = time_str.split(":")
      parts[0] = (parts[0].to_i % 24).to_s.rjust(2, '0')
      parts.join(":")
    end

    def run
      puts "Loading website..."
      visit('/index.html')

      # Get all station names
      find('#from').click()
      all_station_names = all('#from .complete-dropdown .complete-dropdown-item').map(&:text)
      find('body').click() # dimiss the dropdown

      ['weekday', 'saturday', 'sunday'].each { |schedule_name|
        ['NB_TT', 'SB_TT'].each { |direction|
          puts "Testing for #{schedule_name} #{direction}..."
          find(".when-button[value=#{schedule_name}]").click()

          type_name = schedule_name == 'weekday' ? schedule_name : 'weekend'
          puts "Loading test data #{type_name}_#{direction}..."
          schedule = JSON.parse(File.read("test/#{type_name}_#{direction}.json"))

          test_count = 0
          puts "Running tests..."
          schedule.each_with_index { |from, from_index|
            from_name, from_stop_times = from.values_at('name', 'stop_times')
            ASSERT(all_station_names.include?(from_name))
            find('#from').fill_in with: from_name

            schedule.each_with_index { |to, to_index|
              next unless from_index < to_index
              to_name, to_stop_times = to.values_at('name', 'stop_times')
              ASSERT(all_station_names.include?(to_name))
              find('#to').fill_in with: to_name

              ASSERT(from_stop_times.size == to_stop_times.size)

              puts "Testing from #{from_name} to #{to_name}..."

              # Expects: [from_time, to_time]
              expects = from_stop_times.zip(to_stop_times).keep_if { |from_stop, to_stop|
                from_stop['time'] and to_stop['time']
              }.each { |from_stop, to_stop|
                ASSERT(from_stop['service_type'] == to_stop['service_type'])
              }.delete_if { |from_stop, to_stop|
                from_stop['service_type'] == 'SatOnly' and schedule_name != 'saturday'
              }.map { |from_stop, to_stop|
                [from_stop['time'], to_stop['time']]
              }.sort.map { |from_time, to_time|
                [fixTimeFormat(from_time), fixTimeFormat(to_time)]
              }

              # Actuals: [from_time, to_time]
              actuals = all('#result .trip').map { |trip|
                departure_time = trip.find('.departure').text
                arrival_time = trip.find('.arrival').text
                [departure_time, arrival_time]
              }

              # Check results
              ASSERT(expects.size == actuals.size, "Result length mismatch: #{from_name}=>#{to_name} when #{schedule_name}")
              ASSERT(expects == actuals, "Results mismatch: #{expects} <> #{actuals}")

              test_count += 1
            }
          }

          ASSERT(test_count > 100, 'Too few test cases')
          puts "Finish test cases: #{test_count}"
        }
      }
    end
  end

  Runner.new.run
end

desc "Run test against latest test data"
task spec: :download_test_data do
  require 'capybara'
  require 'capybara/dsl'
  require 'rack'

  Capybara.reset!
  Capybara.app = Rack::File.new File.dirname __FILE__
  Capybara.run_server = true
  Capybara.server = :webrick

  class Runner
    include Capybara::DSL

    def run
      visit('/index.html?test=true')
      result = find("#test_result").text
      if result == 'Total failed:0'
        true
      else
        $stderr.puts result
        false
      end
    end
  end

  exit Runner.new.run ? 0 : 1
end

desc "Download GTFS data"
task :download_data do
  require 'tempfile'
  require 'fileutils'

  url = 'http://www.caltrain.com/Assets/GTFS/caltrain/CT-GTFS.zip'
  target_dir = './gtfs/'

  Dir.mktmpdir('gtfs_') { |data_dir|
    Tempfile.open('data.zip') do |temp_file|
      system("curl #{url} -o #{temp_file.path} && unzip -o #{temp_file.path} -d #{data_dir}")
      temp_file.unlink
    end

    FileUtils.remove_dir(target_dir)
    FileUtils.cp_r(data_dir, target_dir)
  }

  # Cleanup \r to \n
  Dir.glob("#{target_dir}/*.txt") { |file|
    content = File.read(file).gsub("\r\n", "\n").gsub("\r", "\n")
    File.write(file, content)
  }

  [:prepare_data, :update_appcache].each do |task|
    Rake::Task[task].invoke
  end
end

desc "Prepare Data"
task :prepare_data do
  require "csv"
  require "json"
  require "plist"

  # Extend CSV
  class CSV
    class Table
      def keep_if(&block)
        delete_if { |item| !yield(item) }
      end
    end
    class Row
      # supports row.attr access method
      def method_missing(meth, *args, &blk)
        if meth =~ /\A(.*)\=\Z/
          self[$1.to_sym] = block_given? ? yield(args[0]) : args[0]
        else
          fetch(meth, *args, &blk)
        end
      end
    end
  end

  def read_CSV(name)
    CSV.read("gtfs/#{name}.txt", headers: true, header_converters: :symbol, converters: :all)
      .each { |item|
        item.service_id = item.service_id.to_s unless item[:service_id].nil?
        item.route_id = item.route_id.to_s unless item[:route_id].nil?
        item.trip_id = item.trip_id.to_s unless item[:trip_id].nil?
      }
  end

  def elem_to_xml(elem)
    case elem
    when Hash
      hash_to_xml(elem)
    when Array
      arr_to_xml(elem)
    else
      elem.to_s
    end
  end
  def arr_to_xml(arr)
    xml = arr.inject("") { |s, elem|
      s + %Q{<elem>#{elem_to_xml(elem)}</elem>}
    }
    "<array>\n#{xml}\n</array>"
  end
  # Transform hash into XML compatible array
  def hash_to_xml(hash)
    xml = hash.inject("") { |s, (k, v)|
      s + "<key>#{k}</key>\n<value>\n#{elem_to_xml(v)}\n</value>"
    }
    "<map>\n#{xml}\n</map>"
  end

  # Read from CSV, prepare it with `block`, write what returns to JSON and PLIST files
  # If multiply names, expected to return a hash as NAME => CONTENT
  def prepare_for(*names, &block)
    raise "block is needed for prepare_for!" unless block_given?
    raise "filename is needed!" if names.size < 1

    csvs = names.map { |name| read_CSV(name) }
    hashes = yield(*csvs)
    raise "prepare_for result has to be a Hash!" unless hashes.is_a? Hash
    hashes.each { |name, hash|
      File.write("data/#{name}.json", hash.to_json)
      File.write("data/#{name}.plist", Plist::Emit.dump(hash))
      # File.write("data/#{name}.xml", %Q{<?xml version="1.0" encoding="UTF-8"?>\n#{hash_to_xml(hash)}\n})
    }
  end


  # From:
  #   routes:
  #     route_id,route_short_name,route_long_name,route_type,route_color
  #     TaSj-16APR, ,Tamien / San Jose Diridon Caltrain Shuttle,3,41AD49
  #     Lo-16APR, ,Local,2,FFFFFF
  #     Li-16APR, ,Limited,2,FEF0B5
  #     Bu-16APR, ,Baby Bullet,2,E31837
  #   trips:
  #     route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,shape_id,wheelchair_accessible,bikes_allowed
  #     TaSj-16APR,CT-16APR-Caltrain-Saturday-02,23a,DIRIDON STATION,23,0,cal_tam_sj,,
  #
  # Find only valid services by route type defined in routes
  valid_service_ids = []
  prepare_for("routes", "trips") do |routes, trips|
    valid_route_ids = routes
      .each { |route|
        ASSERT([2, 3].include? route.route_type)
      }
      .select { |route| route.route_type == 2 } # 2 for Rail, 3 for bus
      .map(&:route_id)

    valid_service_ids = trips
      .select { |trip| valid_route_ids.include? trip.route_id }
      .map(&:service_id)

    {}
  end

  # From:
  #   calendar:
  #     service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
  #     CT-16APR-Caltrain-Weekday-01,1,1,1,1,1,0,0,20160404,20190331
  #     CT-16APR-Caltrain-Saturday-02,0,0,0,0,0,1,0,20140329,20190331
  #     CT-16APR-Caltrain-Sunday-02,0,0,0,0,0,0,1,20140323,20190331
  #   calendar_dates:
  #     service_id,date,holiday_name,exception_type
  #     c_17845_b_none_d_127,20191027,49ers Game,1
  # To:
  #   calendar:
  #     service_id => {weekday: bool, saturday: bool, sunday: bool, start_date: date, end_date: date}
  #     CT-16APR-Caltrain-Weekday-01 => {weekday: false, saturday: true, sunday: false, start_date: 20160404, end_date: 20190331}
  #   calendar_dates:
  #     service_id => [[date, exception_type]]
  #     CT-16APR-Caltrain-Weekday-01 => [[20160530,2]]
  prepare_for("calendar", "calendar_dates") do |calendar, calendar_dates|
    now_date = Time.now.strftime("%Y%m%d").to_i

    calendar = calendar
      .select { |service| valid_service_ids.include? service.service_id }
      .each { |service|
        warn "Outdated service #{service.service_id} ends at #{service.end_date}." if service.end_date < now_date
      }
      .group_by(&:service_id)
      .mapHash { |service_id, items|
        ASSERT(items.size == 1)
        item = items[0]
        weekday_sum = [:monday, :tuesday, :wednesday, :thursday, :friday].inject(0) { |sum, day| sum + item[day]}
        # Weekday should be available all together or none of them. If not, check data.
        ASSERT([0, 5].include? weekday_sum)
        # schedule should match their name
        if service_id.match(/weekday/i)
          ASSERT((weekday_sum == 5 and item.saturday != 1 and item.sunday != 1))
        end
        if service_id.match(/saturday/i)
          unless weekday_sum == 0 and item.saturday == 1 and item.sunday != 1
            warn "Service `#{service_id}` does not match their schedule: #{item}"
            # FAIL()
          end
        end
        if service_id.match(/sunday/i)
          ASSERT((weekday_sum == 0 and item.saturday != 1 and item.sunday == 1))
        end
        {
          weekday: weekday_sum == 5,
          saturday: item.saturday == 1,
          sunday: item.sunday == 1,
          start_date: item.start_date,
          end_date: item.end_date,
        }
      }

    # update valid_service_ids to remove out-dated services
    valid_service_ids = calendar.keys

    dates = calendar_dates
      .each { |service|
        warn "Outdated service_date service #{service.service_id} at #{service.date}." unless valid_service_ids.include? service.service_id
        warn "Outdated service_date #{service.service_id} at #{service.date}." if service.date < now_date
      }
      .group_by(&:service_id)
      .mapHash { |service_id, items|
        items.map { |item| [item.date, item.exception_type] }
      }

    { calendar: calendar, calendar_dates: dates }
  end

  # Remove header and unify station_id by name
  # From:
  #   stop_id,stop_code,stop_name,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,platform_code,wheelchair_boarding
  #   70011,70011,San Francisco Caltrain,37.77639,-122.394992,1,http://www.caltrain.com/stations/sanfranciscostation.html,0,ctsf,NB,1
  #   70012,70012,San Francisco Caltrain,37.776348,-122.394935,1,http://www.caltrain.com/stations/sanfranciscostation.html,0,ctsf,SB,1
  # To:
  #   stop_name => [stop_id1, stop_id2]
  #   "San Francisco" => [70021, 70022]
  prepare_for("stops") do |stops|
    stops = stops
      .each { |item|
        # check data (if its scheme is changed)
        ASSERT((item.stop_name ~ / Caltrain/))
      }
      .select { |item| item.stop_id.is_a?(Integer) }
      .sort_by(&:stop_lat).reverse # sort stations from north to south
      .each { |item|
        # shorten the name and merge San Jose with San Jose Diridon
        item.stop_name.gsub!(/ (Caltrain|Station)/, '').gsub!(/^San Jose$/, 'San Jose Diridon')
      }
      .group_by(&:stop_name)
      .mapHash { |name, items|
        items.map(&:stop_id).sort
      }

    {stops: stops}
  end

  # From:
  #   routes:
  #     route_id,agency_id,route_short_name,route_long_name,route_type,route_color,route_text_color
  #     SHUTTLE,CT,,SHUTTLE,3,,
  #     LOCAL,CT,LOCAL,,2,,
  #     LIMITED,CT,,LIMITED,2,,
  #     BABY BULLET,CT,,BABY BULLET,2,,
  #   trips:
  #     route_id,service_id,trip_id,trip_headsign,direction_id,block_id,shape_id,trip_short_name
  #     SHUTTLE,4951,RTD6320540,DIRIDON STATION,0,,,
  #     SHUTTLE,4951,RTD6320555,DIRIDON STATION,0,,,
  #   stop_times:
  #     trip_id,arrival_time,departure_time,stop_id,stop_sequence
  #     RTD6320540,07:33:00,07:33:00,777403,1
  #     RTD6320540,07:45:00,07:45:00,777402,2
  # To:
  #   routes:
  #     { route_id => { service_id => { trip_id => [[stop_id, arrival_time/departure_time(in seconds)]] } } }
  #     { "Bullet" => { "CT-14OCT-XXX" => { "650770-CT-14OCT-XXX" => [[70012, 29700], ...] } } }
  prepare_for("routes", "trips", "stop_times") do |routes, trips, stop_times|
    # { trip_id => [[stop_id, arrival_time/departure_time(in seconds)]] }
    times = stop_times
      .each { |item|
        # check data (if its scheme is changed)
        ASSERT(item.arrival_time == item.departure_time)
      }
      .group_by(&:trip_id)
      .mapHash { |trip_id, trips_values|
        trips_values
          .sort_by(&:stop_sequence)
          .map { |trip|
            t = trip.arrival_time.split(":").map(&:to_i)
            [trip.stop_id, t[0] * 60 * 60 + t[1] * 60 + t[2]]
          }
      }

    # { route_id => { service_id => { trip_id => ... } } }
    trips = trips
      .group_by(&:route_id)
      .mapHash { |route_id, route_trips|
        route_trips
          .group_by(&:service_id)
          .mapHash { |service_id, service_trips|
            service_trips
              .group_by(&:trip_id)
              .mapHash { |trip_id, trip_trips|
                times[trip_id]
              }
          }
      }

    # { route_id => { service_id => ... } }
    routes = routes
      .select { |route| route.route_type == 2 } # 2 for Rail, 3 for bus
      .group_by(&:route_id)
      .mapHash { |name, routes_values|
        routes_values
          .map(&:route_id)
          .inject({}) { |h, route_id|
            h.merge(trips[route_id])
          }
      }

    { routes: routes }
  end

  puts "Prepared Data."
end

desc "Enable Appcache."
task :enable_appcache do
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

desc "Update Appcache."
task :update_appcache do
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

desc "Minify Files."
task :minify_files do
  require 'tempfile'
  require 'fileutils'

  path = 'javascripts/default.js'
  temp_file = Tempfile.new('default.js')
  begin
    `uglifyjs #{path} -o #{temp_file.path} -c -m`
    FileUtils.mv(temp_file.path, path)
  ensure
    temp_file.close
    temp_file.unlink
  end

  path = 'stylesheets/default.css'
  temp_file = Tempfile.new('default.css')
  begin
    `uglifycss #{path} > #{temp_file.path}`
    FileUtils.mv(temp_file.path, path)
  ensure
    temp_file.close
    temp_file.unlink
  end

  puts "Minified files."
end

desc "Publish"
task :publish do
  def run(cmd)
    res = `#{cmd}`
    unless $?.success?
      warn "#{cmd} failed:\n#{res}\n"
      abort
    end
  end

  begin
    # ensure working dir is clean
    run('[ -n "$(git status --porcelain)" ] && exit 1 || exit 0')

    # push master branch
    run("git checkout master")
    run("git pull origin")
    run("git push origin master:master")

    # push gh-pages branch
    run("git checkout gh-pages")
    run("git pull origin")
    run("git checkout master -- .")
    [:prepare_data, :enable_appcache, :update_appcache, :minify_files].each do |task|
      Rake::Task[task].invoke
    end
    run("git add .")
    run("git commit -m 'Updated at #{Time.now}.'")
    run("git push origin gh-pages:gh-pages")
    run("firebase deploy")
  ensure
    run("git checkout master")
  end
end
