import java.io.FileReader;
import java.util.*;
import java.lang.*;
import static java.lang.Math.abs;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.io.PrintWriter;

import java.util.regex.Pattern;
import java.io.*;
import java.lang.Math;
import java.lang.Number;
import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPSyntaxErrorException;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

class TAXI_AI {
    public static void main(String[] args) {
        try {
            HashMap<String, Point> Map = new HashMap<>();

            //CLIENT
            Scanner file = new Scanner(new FileReader(args[0]));
            String line = file.nextLine();
            String[] data = new String[8];
            line = file.nextLine();
            data = line.split(",");
            Client.X = Double.parseDouble(data[0]);
            Client.Y = Double.parseDouble(data[1]);
            Client.X_dest = Double.parseDouble(data[2]);
            Client.Y_dest = Double.parseDouble(data[3]);
            Client.time = data[4];
            Client.persons = Integer.parseInt(data[5]);
            Client.language = data[6];
            Client.luggage = Integer.parseInt(data[7]);
            file.close();

            // NODES
            file = new Scanner(new FileReader(args[1]));
            line = file.nextLine();
            data = new String[5];
            line = file.nextLine();
            data = line.split(",");
            Point start = new Point(data[0], data[1]);
            Point previous = new Point("0.0", "0.0");
            previous = start;
            int prev_ID = Integer.parseInt(data[2]);
            previous.Road_ID.add(Integer.parseInt(data[2]));
            previous.Road_AA.add(1);
            Map.put((data[0] + data[1]).replace(".", ""), start);
            int iter = 2;
            while (file.hasNext()) {
                line = file.nextLine();
                data = line.split(",");
                Point curr = new Point(data[0], data[1]);
                int curr_ID = Integer.parseInt(data[2]);
                if (Map.containsKey((data[0] + data[1]).replace(".", ""))) { //Node Exists
                    curr = Map.get((data[0] + data[1]).replace(".", ""));
                    if (curr_ID == prev_ID) { //Same ID
                        previous.Neighbors.add(new Neighbor(curr));
                        curr.Neighbors.add(new Neighbor(previous));
                        curr.Road_ID.add(Integer.parseInt(data[2]));
                        curr.Road_AA.add(iter);
                        iter++;
                    }
                    else{
                        curr.Road_ID.add(Integer.parseInt(data[2]));
                        curr.Road_AA.add(1);
                        iter = 2;
                    }
                } else { //Node doesn't exist
                    Map.put((data[0] + data[1]).replace(".", ""), curr);
                    if (curr_ID == prev_ID) { //Same ID
                        previous.Neighbors.add(new Neighbor(curr));
                        curr.Neighbors.add(new Neighbor(previous));
                        curr.Road_ID.add(Integer.parseInt(data[2]));
                        curr.Road_AA.add(iter);
                        iter++;
                    }
                    else{
                        curr.Road_ID.add(Integer.parseInt(data[2]));
                        curr.Road_AA.add(1);
                        iter = 2;
                    }
                }
                previous = curr;
                prev_ID = curr_ID;
            }
            file.close();
            for (Map.Entry<String, Point> entry : Map.entrySet()) {
                double PointX = entry.getValue().X;
                double PointY = entry.getValue().Y;
                for (int j = 0; j < entry.getValue().Neighbors.size(); j++) {
                    double NeighX = entry.getValue().Neighbors.get(j).target.X;
                    double NeighY = entry.getValue().Neighbors.get(j).target.Y;
                    double dist = abs(NeighX - PointX) + abs(NeighY - PointY);
                    entry.getValue().Neighbors.get(j).cost = dist;
                }
            }

            //TAXIS
            HashMap<String, Point> Taxi_Map = new HashMap<>();
            HashMap<String, Point> Original_Taxi_Map = new HashMap<>();
            file = new Scanner(new FileReader(args[2]));
            line = file.nextLine();
            TreeMap<Double, String> RateList = new TreeMap<Double, String>(Collections.reverseOrder());
            while (file.hasNext()) {
                data = new String[9];
                line = file.nextLine();
                data = line.split(",");
                if (!(data[3].equals("yes") && (Double.parseDouble(data[6]) > 7.0) && data[5].contains(Client.language))){
                    continue;
                }
                RateList.put(Double.parseDouble(data[6]),data[2]);
                Original_Taxi_Map.put(data[2], new Point(data[0],data[1]));
                double min = 9999999.9;
                Point Taxi_Point = new Point("0.0", "0.0");
                for (Map.Entry<String, Point> entry : Map.entrySet()) {
                    Point point = entry.getValue();
                    double diff = abs(Double.parseDouble(data[0]) - point.X) + abs(Double.parseDouble(data[1]) - point.Y);
                    if (diff < min) {
                        min = diff;
                        Taxi_Point = point;
                    }
                }
                Taxi_Map.put(data[2], Taxi_Point);
            }
            file.close();

            double min = 9999999.9;
            Point Client_Point = new Point("0.0", "0.0");
            for (Map.Entry<String, Point> entry : Map.entrySet()) {
                Point point = entry.getValue();
                double diff = abs(Client.X - point.X) + abs(Client.Y - point.Y);
                if (diff < min) {
                    min = diff;
                    Client_Point = point;
                }
            }

            min = 9999999.9;
            Point Client_Dest_Point = new Point("0.0", "0.0");
            for (Map.Entry<String, Point> entry : Map.entrySet()) {
                Point point = entry.getValue();
                double diff = abs(Client.X_dest - point.X) + abs(Client.Y_dest - point.Y);
                if (diff < min) {
                    min = diff;
                    Client_Dest_Point = point;
                }
            }

            KnowledgeBase prolog_base = new KnowledgeBase(Map, args[3], args[4], "," );
            prolog_base.Construct();

            Q_Cap = Integer.parseInt(args[5]);
            PrintWriter stats_writer = new PrintWriter("Taxi-Client_Stats_TAXI_AI_"+Q_Cap+".out", "UTF-8");
            PrintWriter writer = new PrintWriter("Taxi-Client_Map_TAXI_AI_"+Q_Cap+".kml", "UTF-8");
            PrintWriter client_writer = new PrintWriter("Client_TAXI_AI_"+Q_Cap+".txt", "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
            writer.println("<Document>");
            writer.println("<name>"+"Ταξί-Πελάτης ~ Μέγιστο Μέτωπο Αναζήτησης: "+Q_Cap+"</name>");
            writer.println("<Style id=\"green\">");
            writer.println("<LineStyle>");
            writer.println("<color>ff009900</color>");
            writer.println("<width>4</width>");
            writer.println("</LineStyle>");
            writer.println("</Style>");
            writer.println("<Style id=\"red\">");
            writer.println("<LineStyle>");
            writer.println("<color>ff0000ff</color>");
            writer.println("<width>4</width>");
            writer.println("</LineStyle>");
            writer.println("</Style>");
            HashMap<Double, Route> Routes = new HashMap<>();

            //SEARCH TAXI-CLIENT ROUTES
            for (Map.Entry<String, Point> entry : Taxi_Map.entrySet()) {
                String key = entry.getKey();
                double total_cost=0;
                steps=0;
                max_queue_size=-1;
                AstarSearch(Taxi_Map.get(key), Client_Point);  //A* SEARCH
                ArrayList<Point> path = printPath(Client_Point);
                total_cost = path.get(path.size()-1).g;
                stats_writer.println("Taxi ID: "+key+" Steps: "+steps+" Real Maximum Queue Size: "+max_queue_size+" Distance: "+distFrom(total_cost)+" km");
                Routes.put(distFrom(total_cost),new Route(key,(ArrayList<Point>)path.clone()));
                for (Map.Entry<String, Point> entryy : Map.entrySet()) {
                	Point val = entryy.getValue();
                	val.g = 0;
                    val.f = 0;
                	val.parent = null;
                }
                Client_Point.g=0;
                Client_Point.f=0;
                Client_Point.parent=null;
            }

            //PRINT TAXI-CLIENT ROUTES
            Map<Double, Route> Sorted_Routes = new TreeMap<>(Routes);
            Set<Double> taxis = Sorted_Routes.keySet();
            Iterator itr = taxis.iterator();
            Object set_iter = itr.next();
            Route taxi = Sorted_Routes.get(set_iter);
            client_writer.println("Closest 4 Available Taxis by Distance:");
            client_writer.println("");
            stats_writer.println("Closest Taxi to Client: "+taxi.ID);
            writer.println("<Placemark>");
            writer.println("<name>Taxi "+taxi.ID+"</name>");
            int taxi_counter = 0;
            client_writer.println(taxi.ID+": "+String.format("%.02f", set_iter)+ "km");
            taxi_counter++;
            writer.println("<styleUrl>#green</styleUrl>");
            writer.println("<LineString>");
            writer.println("<altitudeMode>relative</altitudeMode>");
            writer.println("<coordinates>");
            writer.println(Original_Taxi_Map.get(taxi.ID).X + "," + Original_Taxi_Map.get(taxi.ID).Y);
            for (int i = 0; i < taxi.Path.size(); i++) {
                writer.println(taxi.Path.get(i).X + "," + taxi.Path.get(i).Y);
            }
            writer.println(Client.X+ "," +Client.Y);
            writer.println("</coordinates>");
            writer.println("</LineString>");
            writer.println("</Placemark>");
            while (itr.hasNext()){
                set_iter = itr.next();
                taxi = Sorted_Routes.get(set_iter);
                writer.println("<Placemark>");
                writer.println("<name>Taxi "+taxi.ID+"</name>");
                if (taxi_counter <4) {
                    client_writer.println(taxi.ID + ": " + String.format("%.02f", set_iter) + "km");
                    taxi_counter++;
                }
                writer.println("<styleUrl>#red</styleUrl>");
                writer.println("<LineString>");
                writer.println("<altitudeMode>relative</altitudeMode>");
                writer.println("<coordinates>");
                writer.println(Original_Taxi_Map.get(taxi.ID).X + "," + Original_Taxi_Map.get(taxi.ID).Y);
                for (int i = 0; i < taxi.Path.size(); i++) {
                    writer.println(taxi.Path.get(i).X + "," + taxi.Path.get(i).Y);
                }
                writer.println(Client.X+ "," +Client.Y);
                writer.println("</coordinates>");
                writer.println("</LineString>");
                writer.println("</Placemark>");
            }
            writer.println("</Document>");
            writer.println("</kml>");
            writer.close();
            stats_writer.close();
            client_writer.println("");
            client_writer.println("Closest 4 Available Taxis by Rating:");
            client_writer.println("");
            Set set = RateList.entrySet();
            Iterator i = set.iterator();
            // Display elements
            while(i.hasNext() && taxi_counter>0) {
                Map.Entry me = (Map.Entry)i.next();
                client_writer.println(me.getValue()+": "+me.getKey());
                taxi_counter--;
            }
            client_writer.close();

            //SEARCH CLIENT-DESTINATION ROUTE

            Iterator entries = Map.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Point value = (Point)entry.getValue();
                value.h = abs(value.X - Client.X_dest) + abs(value.Y - Client.Y_dest);
            }

            double total_cost=0;
            steps=0;
            max_queue_size=-1;
            AstarSearch(Client_Point, Client_Dest_Point);  //A* SEARCH
            ArrayList<Point> path = printPath(Client_Dest_Point);
            total_cost = path.get(path.size()-1).g;
            stats_writer = new PrintWriter("Client-Destination_Stats_TAXI_AI_"+Q_Cap+".out", "UTF-8");
            stats_writer.println("Steps: "+steps+" Real Maximum Queue Size: "+max_queue_size+" Distance: "+distFrom(total_cost)+" km");
            stats_writer.close();
            Route Final_Route = new Route("Client",(ArrayList<Point>)path.clone());
            writer = new PrintWriter("Client-Destination_Map_TAXI_AI_"+Q_Cap+".kml", "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
            writer.println("<Document>");
            writer.println("<name>"+"Πελάτης-Προορισμός ~ Μέγιστο Μέτωπο Αναζήτησης: "+Q_Cap+"</name>");
            writer.println("<Style id=\"blue\">");
            writer.println("<LineStyle>");
            writer.println("<color>7dff0000</color>");
            writer.println("<width>4</width>");
            writer.println("</LineStyle>");
            writer.println("</Style>");
            writer.println("<Placemark>");
            writer.println("<name>Πελάτης</name>");
            writer.println("<styleUrl>#blue</styleUrl>");
            writer.println("<LineString>");
            writer.println("<altitudeMode>relative</altitudeMode>");
            writer.println("<coordinates>");
            writer.println(Client.X + "," + Client.Y);
            for (int p = 0; p < Final_Route.Path.size(); p++) {
                writer.println(Final_Route.Path.get(p).X + "," + Final_Route.Path.get(p).Y);
            }
            writer.println(Client.X_dest+ "," +Client.Y_dest);
            writer.println("</coordinates>");
            writer.println("</LineString>");
            writer.println("</Placemark>");
            writer.println("</Document>");
            writer.println("</kml>");
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int Q_Cap;
    public static PriorityQueue<Point> queue = new PriorityQueue<Point>();
    public static int steps;
    public static int max_queue_size;

    public static class Client_Class{
        double X;
        double Y;
        double X_dest;
        double Y_dest;
        String time;
        int persons;
        String language;
        int luggage;
    }
    public static Client_Class Client = new Client_Class();

    public static class Point {
        double X;
        double Y;
        double g;
        double h;
        double f = 0;
        ArrayList<Integer> Road_ID;
        ArrayList<Integer> Road_AA;
        ArrayList<Neighbor> Neighbors;
        Point parent;

        Point(String X, String Y) {
            this.X = Double.parseDouble(X);
            this.Y = Double.parseDouble(Y);
            this.h = abs(Double.parseDouble(X) - Client.X) + abs(Double.parseDouble(Y) - Client.Y);
            this.Neighbors = new ArrayList<Neighbor>();
            this.Road_ID = new ArrayList<Integer>();
            this.Road_AA = new ArrayList<Integer>();
        }
    }

    public static class Neighbor {
        double cost;
        Point target;

        Neighbor(Point targetPoint) {
            target = targetPoint;
        }
    }

    public static class Route{
        String ID;
        ArrayList<Point> Path;
        Route (String id,ArrayList<Point> path ){
            ID=id;
            Path=path;
        }
    }


    public static void AstarSearch(Point source, Point goal) throws Exception {

        Set<Point> explored = new HashSet<Point>();

        queue = new PriorityQueue<Point>(Q_Cap,
                new Comparator<Point>() {
                    //override compare method
                    public int compare(Point i, Point j) {
                        if (i.f > j.f) {
                            return 1;
                        } else if (i.f < j.f) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                }
        );

        JIPEngine jip = new JIPEngine();
        try {
            jip.consultFile("prolog_base_1.pl");
            jip.consultFile("prolog_base_2.pl");
            jip.consultFile("prolog_rules.pl");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        JIPTermParser parser = jip.getTermParser();
        JIPQuery jipQuery;
        JIPTerm term;

        //cost from start
        source.g = 0;
        queue.add(source);
        boolean found = false;
        while ((!queue.isEmpty()) && (!found)) {
            steps++;
            if (queue.size()> max_queue_size){max_queue_size = queue.size();}
            //the node in having the lowest f value
            Point current = queue.poll();
            explored.add(current);
            //goal found
            if (current.X == goal.X && current.Y == goal.Y) {
                found = true;
            }
            String current_id = (String.valueOf(current.X)+String.valueOf(current.Y)).replace(".", "");
            //check every child of current node
            for (Neighbor n : current.Neighbors) {
                String neigh_id = (String.valueOf(n.target.X)+String.valueOf(n.target.Y)).replace(".", "");

                jipQuery = jip.openSynchronousQuery(parser.parseTerm("canMoveFromTo(" + current_id + "," + neigh_id + ", Road )."));
                term = jipQuery.nextSolution();

                if (term != null) {
                    Point child = n.target;
                    double cost = n.cost;
                    double temp_g = current.g + cost;
                    ArrayList<Integer> commonIDlst = new ArrayList<Integer>(current.Road_ID);
                    commonIDlst.retainAll(child.Road_ID);
                    Integer commonID = commonIDlst.get(0);
                    jipQuery = jip.openSynchronousQuery(parser.parseTerm("priority("+commonID+","+(Client.time).split(":")[0]+", Z)."));
                    term = jipQuery.nextSolution();
                    double extra_cost;
                    if (term != null) {
                        extra_cost = Double.parseDouble(term.getVariablesTable().get("Z").toString());
                    }
                    else {
                        extra_cost=0.0;
                    }
                    double temp_f = temp_g + child.h + extra_cost;
                    /*if child node has been evaluated and
                    the newer f is higher, skip*/
                    if ((explored.contains(child)) &&
                            (temp_f >= child.f)) {
                        continue;
                    }
                    /*else if child node is not in queue or
                    newer f is lower*/
                    else if ((!queue.contains(child)) ||
                            (temp_f < child.f)) {
                        child.parent = current;
                        child.g = temp_g;
                        child.f = temp_f;
                        if (queue.contains(child)) {
                            queue.remove(child);
                        }
                        queue.add(child);
                        if (queue.size() > Q_Cap) {
                            queue = removelast(queue);
                        }
                    }
                }
                else{
                   continue;
                }
            }

        }

    }
    public static ArrayList<Point> printPath(Point target){
        ArrayList<Point> path = new ArrayList<Point>();

        for(Point point = target; point!=null; point = point.parent){
            path.add(point);
        }
        Collections.reverse(path);
        return path;
    }
    public static PriorityQueue removelast(PriorityQueue pq)
    {
        PriorityQueue pqnew = new PriorityQueue<Point>(Q_Cap,
                new Comparator<Point>() {
                    //override compare method
                    public int compare(Point i, Point j) {
                        if (i.f > j.f) {
                            return 1;
                        } else if (i.f < j.f) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                }
        );

        while(pq.size() > 1)
        {
            pqnew.add(pq.poll());
        }

        pq.clear();
        return pqnew;
    }
    public static double distFrom(double lat) {
        double earthRadius = 6371.0; //meters
        double dLat = Math.toRadians(lat);
        double sindLat = Math.sin(dLat / 2);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = ((double) (earthRadius * c));
        return dist;
    }

    public static class KnowledgeBase {
        private HashMap<String, Point> mapping;
        private String inputFilename_lines, inputFilename_trafficInfo, delimiter;
        KnowledgeBase(HashMap<String, Point> the_mapping, String the_inputFilename_lines, String the_inputFilename_trafficInfo, String delim) {
            mapping = the_mapping;
            inputFilename_lines = the_inputFilename_lines;
            inputFilename_trafficInfo = the_inputFilename_trafficInfo;
            delimiter = ",";
        }

        public void Construct() {
            try {
                PrintWriter wr = new PrintWriter("prolog_base_1.pl", "utf-8");
                for (String key : mapping.keySet()) {
                    String keyID = key;
                    ArrayList<Integer> position_in_line = mapping.get(key).Road_AA;
                    ArrayList<Integer> line = mapping.get(key).Road_ID;
                    for (int j = 0; j < position_in_line.size(); j++) {
                        wr.println("pointRoadPosition(" + keyID + "," + line.get(j) + "," + position_in_line.get(j) + ").");
                    }
                }
                wr.close();
                String id_Line;
                String line_beforeSplit;
                String[] line_info;
                File input_lines = new File(inputFilename_lines);
                BufferedReader rd = new BufferedReader(new FileReader(input_lines));
                line_beforeSplit = rd.readLine();
                line_beforeSplit = rd.readLine();
                int id_column = 0;
                int road_type_column = 1;
                int railway_column = 7;
                int access_column = 9;
                int natural_column = 10;
                int barrier_column = 11;
                int tunnel_column = 12;
                int water_column = 15;
                int oneway_pos = 3;
                int service_cost = 7;
                int primary_cost = 3;
                int motor_cost = 1;
                int residential_cost = 5;
                int trunk_cost = 2;
                int secondary_cost = 4;
                int tertiary_cost = 6;
                int unclassified_cost = 8;
                wr = new PrintWriter("prolog_base_2.pl", "utf-8");
                boolean flag_forbidden = false;
                while (line_beforeSplit != null) {
                    flag_forbidden = false;
                    line_info = line_beforeSplit.split(Pattern.quote(","));
                    if (line_info.length <= 0) {
                        line_beforeSplit = rd.readLine();
                        continue;
                    }
                    id_Line = line_info[id_column];
                    if (line_info.length > road_type_column) {
                        if (line_info[road_type_column].equals("footway") || line_info[road_type_column].equals("pedestrian") || line_info[road_type_column].equals("path") || line_info[road_type_column].equals("elevator") || line_info[road_type_column].equals("bridleway") || line_info[road_type_column].equals("cycleway") || line_info[road_type_column].equals("proposed") || line_info[road_type_column].equals("construction")) {
                            wr.println("forbidden(" + id_Line + ").");
                            flag_forbidden = !flag_forbidden;
                        }
                        else if (line_info[road_type_column].equals("service")) {
                            wr.println("highway(" + id_Line + "," + service_cost + ").");
                        } else if (line_info[road_type_column].equals("primary") || line_info[road_type_column].equals("primary_link")) {
                            wr.println("highway(" + id_Line + "," + primary_cost + ").");
                        } else if (line_info[road_type_column].equals("motorway") || line_info[road_type_column].equals("motorway_link")) {
                            wr.println("highway(" + id_Line + "," + motor_cost + ").");
                        } else if (line_info[road_type_column].equals("trunk") || line_info[road_type_column].equals("trunk_link")) {
                            wr.println("highway(" + id_Line + "," + trunk_cost + ").");
                        } else if (line_info[road_type_column].equals("secondary") || line_info[road_type_column].equals("secondary_link")) {
                            wr.println("highway(" + id_Line + "," + secondary_cost + ").");
                        } else if (line_info[road_type_column].equals("residential")) {
                            wr.println("highway(" + id_Line + "," + residential_cost + ").");
                        } else if (line_info[road_type_column].equals("tertiary")) {
                            wr.println("highway(" + id_Line + "," + tertiary_cost + ").");
                        } else if (line_info[road_type_column].equals("unclassified")) {
                            wr.println("highway(" + id_Line + "," + unclassified_cost + ").");
                        } else if (line_info[road_type_column].isEmpty()) {
                            wr.println("highway(" + id_Line + "," + residential_cost + ").");
                        }
                    }
                    if (!flag_forbidden && line_info.length > railway_column && line_info[railway_column] != null) {
                        wr.println("forbidden(" + id_Line + ").");
                        flag_forbidden = !flag_forbidden;
                    }
                    if (!flag_forbidden && line_info.length > access_column && (line_info[access_column].equals("no") || line_info[access_column].equals("private"))) {
                        wr.println("forbidden(" + id_Line + ").");
                        flag_forbidden = !flag_forbidden;
                    }
                    if (!flag_forbidden && line_info.length > natural_column && line_info[natural_column] != null) {
                        wr.println("forbidden(" + id_Line + ").");
                        flag_forbidden = !flag_forbidden;
                    } else if (!flag_forbidden && line_info.length > barrier_column && line_info[barrier_column] != null) {
                        wr.println("forbidden(" + id_Line + ").");
                        flag_forbidden = !flag_forbidden;
                    } else if (!flag_forbidden && line_info.length > tunnel_column &&
                            (line_info[tunnel_column].equals("no") || line_info[tunnel_column].equals("building_passage") || line_info[tunnel_column].equals("culvert"))) {
                        wr.println("forbidden(" + id_Line + ").");
                        flag_forbidden = !flag_forbidden;
                    } else if (!flag_forbidden && line_info.length > water_column && line_info[water_column] != null) {
                        wr.println("forbidden(" + id_Line + ").");
                        flag_forbidden = !flag_forbidden;
                    }
                    if (line_info.length > oneway_pos && line_info[oneway_pos].equals("yes")) {
                        wr.println("oneway(" + id_Line + ",1).");
                    } else if (line_info.length > oneway_pos && line_info[oneway_pos].equals("-1")) {
                        wr.println("oneway(" + id_Line + ",-1).");
                    } else if (line_info.length > oneway_pos && line_info[oneway_pos].equals("no")) {
                        wr.println("twoway(" + id_Line + ").");
                    } else {
                        wr.println("twoway(" + id_Line + ").");
                    }
                    line_beforeSplit = rd.readLine();
                }
                rd.close();
                File input_traffic = new File(inputFilename_trafficInfo);
                BufferedReader rd_traf = new BufferedReader(new FileReader(input_traffic));
                line_beforeSplit = rd_traf.readLine();
                line_beforeSplit = rd_traf.readLine();
                int traffic_info_column = 2;
                String[] traffic_during_day;
                String[] traffic_timer = new String[]{"9-11", "13-15", "17-19"};
                int low = 1;
                int medium = 2;
                int high = 3;

                while (line_beforeSplit != null) {
                    line_info = line_beforeSplit.split(delimiter);
                    id_Line = (line_info[id_column]);
                    if (line_info.length > traffic_info_column && !(line_info[traffic_info_column].equals(""))) {
                        traffic_during_day = line_info[traffic_info_column].split(Pattern.quote("|"));*
                        for (int i = 0; i < 3; i++) {
                            if (traffic_during_day.length > i && traffic_during_day[i].endsWith("low")) {
                                wr.println("current_traffic(" + id_Line + "," + traffic_timer[i] + "," + low + ").");
                            }
                            if (traffic_during_day.length > i && traffic_during_day[i].endsWith("medium")) {
                                wr.println("current_traffic(" + id_Line + "," + traffic_timer[i] + "," + medium + ").");
                            }
                            if (traffic_during_day.length > i && traffic_during_day[i].endsWith("high")) {
                                wr.println("current_traffic(" + id_Line + "," + traffic_timer[i] + "," + high + ").");
                            }
                        }
                    }
                    line_beforeSplit = rd_traf.readLine();
                }
                rd_traf.close();
                wr.close();
                wr = new PrintWriter("prolog_rules.pl", "utf-8");
                wr.println("canMoveFromTo(X, Y, R) :- " + " pointRoadPosition(X, L, P1), " + " pointRoadPosition(Y, L, P2), " + " P2 > P1, " +" \\+ forbidden(L), " + " oneway(L,1), R is L. ");
                wr.println("canMoveFromTo(X, Y, R) :- " +" pointRoadPosition(X, L, P1), " +" pointRoadPosition(Y, L, P2), " +" P1 > P2, " +" \\+ forbidden(L), " +" oneway(L,-1), R is L. ");
                wr.println("canMoveFromTo(X, Y, R) :- " +" pointRoadPosition(X, L, P1), " +" pointRoadPosition(Y, L, P2), " +" \\+ forbidden(L), " +" twoway(L), R is L. ");
                wr.println("morning(T) :- T > 8, T < 12.");
                wr.println("noon(T) :- T > 12, T < 16.");
                wr.println("evening(T) :- T > 16, T < 20.");
                wr.println("priority(R, T, Z) :- " +"  (highway(R, ValueHighway) -> RES1 is ValueHighway ; RES1 is 0 ), " +"  (morning(T) -> (current_traffic(R, 9-11, ValueM) -> RES2 is ValueM ; RES2 is 0 )), " +"  (noon(T) -> (current_traffic(R, 13-15, ValueN) -> RES3 is ValueN ; RES3 is 0 )), " + "  (evening(T) -> (current_traffic(R, 17-19, ValueE) -> RES4 is ValueE ; RES4 is 0 )), " +" Z is RES1 + RES2 + RES3 + RES4, !. " );

                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}