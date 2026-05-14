package com.tomaytotomato.data.solr.curator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maps book categories to famous real-world bookshops and libraries, giving each curated book
 * a plausible geographic location for geospatial query demos.
 */
final class BookshopLocations {

  record Location(double latitude, double longitude, String name) {}

  private static final List<Location> GENERAL_LOCATIONS = List.of(
      new Location(51.5219, -0.1311, "Waterstones Piccadilly, London"),
      new Location(40.7528, -73.9822, "Strand Bookstore, New York"),
      new Location(48.8530, 2.3471, "Shakespeare & Company, Paris"),
      new Location(55.9533, -3.1883, "Blackwell's, Edinburgh"),
      new Location(-33.8784, 151.2103, "Gleebooks, Sydney"),
      new Location(52.3702, 4.8952, "American Book Center, Amsterdam"),
      new Location(35.6762, 139.6503, "Kinokuniya, Tokyo"),
      new Location(41.3874, 2.1686, "La Central, Barcelona"),
      new Location(45.4642, 9.1900, "Feltrinelli, Milan"),
      new Location(50.0875, 14.4214, "Globe Bookstore, Prague")
  );

  private static final Map<String, List<Location>> CATEGORY_LOCATIONS = Map.ofEntries(
      Map.entry("Fiction", List.of(
          new Location(51.5219, -0.1311, "Waterstones Piccadilly, London"),
          new Location(48.8530, 2.3471, "Shakespeare & Company, Paris"),
          new Location(40.7484, -73.9856, "The Rizzoli Bookstore, New York"),
          new Location(37.7866, -122.4070, "City Lights Books, San Francisco")
      )),
      Map.entry("Science Fiction", List.of(
          new Location(37.7866, -122.4070, "City Lights Books, San Francisco"),
          new Location(51.5219, -0.1311, "Waterstones Piccadilly, London"),
          new Location(45.5231, -122.6765, "Powell's City of Books, Portland")
      )),
      Map.entry("Fantasy", List.of(
          new Location(51.7520, -1.2577, "Blackwell's, Oxford"),
          new Location(55.9533, -3.1883, "Blackwell's, Edinburgh"),
          new Location(53.3419, -6.2615, "Hodges Figgis, Dublin")
      )),
      Map.entry("History", List.of(
          new Location(51.5074, -0.0991, "Hatchards, London"),
          new Location(38.8977, -77.0066, "Politics and Prose, Washington DC"),
          new Location(48.8462, 2.3372, "Librairie Gallimard, Paris")
      )),
      Map.entry("Science", List.of(
          new Location(42.3601, -71.0589, "Harvard Book Store, Cambridge"),
          new Location(51.7520, -1.2577, "Blackwell's, Oxford"),
          new Location(52.2053, 0.1218, "Cambridge University Press Bookshop")
      )),
      Map.entry("Philosophy", List.of(
          new Location(37.9715, 23.7267, "Politeia Books, Athens"),
          new Location(48.8530, 2.3471, "Shakespeare & Company, Paris"),
          new Location(52.5200, 13.4050, "Dussmann das KulturKaufhaus, Berlin")
      )),
      Map.entry("Biography", List.of(
          new Location(40.7528, -73.9822, "Strand Bookstore, New York"),
          new Location(51.5219, -0.1311, "Waterstones Piccadilly, London"),
          new Location(33.7490, -84.3880, "A Cappella Books, Atlanta")
      )),
      Map.entry("Poetry", List.of(
          new Location(48.8530, 2.3471, "Shakespeare & Company, Paris"),
          new Location(53.3419, -6.2615, "Hodges Figgis, Dublin"),
          new Location(55.9533, -3.1883, "Blackwell's, Edinburgh")
      )),
      Map.entry("Art", List.of(
          new Location(48.8606, 2.3376, "Librairie du Musée du Louvre, Paris"),
          new Location(40.7794, -73.9632, "The Met Store, New York"),
          new Location(51.5089, -0.0754, "Tate Modern Shop, London")
      )),
      Map.entry("Horror", List.of(
          new Location(41.0534, -73.5387, "Cryptozoic Books, Sleepy Hollow"),
          new Location(51.5219, -0.1311, "Waterstones Piccadilly, London"),
          new Location(45.5231, -122.6765, "Powell's City of Books, Portland")
      )),
      Map.entry("True Crime", List.of(
          new Location(40.7528, -73.9822, "Strand Bookstore, New York"),
          new Location(51.5074, -0.0991, "Hatchards, London"),
          new Location(41.8781, -87.6298, "Myopic Books, Chicago")
      )),
      Map.entry("Computers", List.of(
          new Location(37.4419, -122.1430, "Books Inc, Palo Alto"),
          new Location(47.6062, -122.3321, "Ada's Technical Books, Seattle"),
          new Location(37.7749, -122.4194, "City Lights Books, San Francisco")
      )),
      Map.entry("Business", List.of(
          new Location(40.7528, -73.9822, "Strand Bookstore, New York"),
          new Location(51.5154, -0.1410, "Waterstones Gower Street, London"),
          new Location(1.2839, 103.8515, "Kinokuniya, Singapore")
      )),
      Map.entry("Religion", List.of(
          new Location(41.9029, 12.4534, "Libreria Editrice Vaticana, Vatican City"),
          new Location(31.7683, 35.2137, "Munther's Bookshop, Jerusalem"),
          new Location(51.5074, -0.0991, "Hatchards, London")
      )),
      Map.entry("Psychology", List.of(
          new Location(48.2082, 16.3738, "Freud Museum Shop, Vienna"),
          new Location(42.3601, -71.0589, "Harvard Book Store, Cambridge"),
          new Location(51.5219, -0.1311, "Waterstones Piccadilly, London")
      )),
      Map.entry("Cooking", List.of(
          new Location(48.8530, 2.3471, "Librairie Gourmande, Paris"),
          new Location(40.7234, -73.9932, "Bonnie Slotnick Cookbooks, New York"),
          new Location(51.5074, -0.1311, "Books for Cooks, London")
      ))
  );

  private BookshopLocations() {}

  static Location forCategory(String primaryCategory) {
    var locations = CATEGORY_LOCATIONS.getOrDefault(primaryCategory, GENERAL_LOCATIONS);
    var index = ThreadLocalRandom.current().nextInt(locations.size());
    return locations.get(index);
  }
}
