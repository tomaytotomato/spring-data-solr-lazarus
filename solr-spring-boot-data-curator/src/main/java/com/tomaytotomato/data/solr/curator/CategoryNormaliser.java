package com.tomaytotomato.data.solr.curator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class CategoryNormaliser {

  private static final Map<Pattern, List<String>> RULES = new LinkedHashMap<>();

  static {
    // Specific compound genres FIRST — before any general word matches
    rule("(?i)true crime", "True Crime");
    rule("(?i)science fiction|sci-fi", "Science Fiction");
    rule("(?i)fantasy fiction|fantasy", "Fantasy");
    rule("(?i)historical fiction", "Fiction", "History");
    rule("(?i)horror tales?|horror stor|ghost stor", "Horror");
    rule("(?i)adventure stor", "Fiction", "Adventure");
    rule("(?i)romance|love stor", "Fiction", "Romance");
    rule("(?i)political science", "Politics");

    // Exact-match genres
    rule("^Fiction$", "Fiction");
    rule("^Juvenile Fiction$", "Fiction", "Children's");
    rule("^Young Adult Fiction$", "Fiction", "Young Adult");
    rule("(?i)english fiction|american fiction|domestic fiction|classical fiction", "Fiction");

    // General keyword genres — broader patterns that would swallow specifics above
    rule("(?i)detective|mystery|thriller|crime|suspense", "Mystery & Thriller");
    rule("(?i)biography|autobiography|memoir", "Biography");
    rule("^History$|(?i)^history ", "History");
    rule("(?i)literary criticism|literary collections", "Literary Criticism");
    rule("^Philosophy$", "Philosophy");
    rule("^Comics & Graphic Novels$|(?i)graphic novel|comic book", "Comics & Graphic Novels");
    rule("^Religion$|(?i)bible|christian|theology|spiritual", "Religion");
    rule("(?i)^Drama$|english drama|french drama|plays", "Drama");
    rule("^Poetry$|(?i)poems|english poetry|love poetry", "Poetry");
    rule("^Science$|(?i)physics|chemistry|biology|astronomy|cosmolog|mathemat|zoolog", "Science");
    rule("^Computers$|(?i)computer science|programming|software", "Technology");
    rule("(?i)business|econom|finance|management|entrepreneur", "Business");
    rule("^Psychology$|(?i)psycholog|psychoanaly", "Psychology");
    rule("(?i)^Cooking$|cookbook|recipe|culinary|chocolate|brewing", "Cooking");
    rule("(?i)^Art$|architecture|photography|design|painting", "Art & Design");
    rule("(?i)^Music$|musician|rock musician", "Music");
    rule("(?i)^Travel$", "Travel");
    rule("(?i)^Self-Help$|self help|personal", "Self-Help");
    rule("(?i)^Health & Fitness$|^Medical$|diet|exercise|disease|cancer", "Health & Medicine");
    rule("(?i)^Education$|teaching|study aids|college", "Education");
    rule("(?i)politic|government|democracy", "Politics");
    rule("(?i)social science|sociolog|anthropolog", "Social Science");
    rule("(?i)nature|garden|environment|animal|pet|bird|cat|dog", "Nature");
    rule("(?i)sports|recreation|game|athletic", "Sports & Games");
    rule("(?i)humor|humour|wit|comic", "Humour");
    rule("(?i)^Horror$", "Horror");
    rule("(?i)performing arts|theater|theatre|film|cinema|acting", "Performing Arts");
    rule("(?i)family|relationship|parenting|marriage", "Family & Relationships");
    rule("(?i)law$|legal|justice", "Law");
    rule("(?i)^Juvenile Nonfiction$|children's stor", "Children's");
    rule("(?i)language|linguistics|grammar|foreign language", "Language");
    rule("(?i)reference|encyclopedia", "Reference");
    rule("(?i)crafts|hobbi", "Crafts & Hobbies");
    rule("(?i)war|military|battle", "History", "Military");
  }

  private CategoryNormaliser() {}

  static List<String> normalise(String rawCategory) {
    if (rawCategory == null || rawCategory.isBlank()) {
      return List.of("General");
    }

    var trimmed = rawCategory.trim();

    for (var entry : RULES.entrySet()) {
      if (entry.getKey().matcher(trimmed).find()) {
        return entry.getValue();
      }
    }

    return List.of("General");
  }

  static String primaryGenre(String rawCategory) {
    return normalise(rawCategory).getFirst();
  }

  private static void rule(String pattern, String... genres) {
    RULES.put(Pattern.compile(pattern), List.of(genres));
  }
}
