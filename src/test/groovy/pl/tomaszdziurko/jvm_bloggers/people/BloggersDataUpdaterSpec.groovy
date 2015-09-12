package pl.tomaszdziurko.jvm_bloggers.people

import pl.tomaszdziurko.jvm_bloggers.people.json_data.BloggerEntry
import pl.tomaszdziurko.jvm_bloggers.utils.NowProvider
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class BloggersDataUpdaterSpec extends Specification {

    PersonRepository personRepository = Mock(PersonRepository)

    @Subject
    BloggersDataUpdater bloggersDataUpdater = new BloggersDataUpdater(personRepository, new NowProvider())

    @Unroll
    def "Should check equality of Person and BloggerEntry"() {
        when:
            boolean isEqual = bloggersDataUpdater.isEqual(person, bloggerEntry)
        then:
            isEqual == expectedResult
        where:
            person                                                    | bloggerEntry                               | expectedResult
            new Person("name", "rss", "twitter", LocalDateTime.now()) | new BloggerEntry("name", "rss", "twitter") | true
            new Person("Xame", "rss", "twitter", LocalDateTime.now()) | new BloggerEntry("name", "rss", "twitter") | false
            new Person("name", "Xss", "twitter", LocalDateTime.now()) | new BloggerEntry("name", "rss", "twitter") | false
            new Person("name", "rss", "Xwitter", LocalDateTime.now()) | new BloggerEntry("name", "rss", "twitter") | false
            new Person("namX", "rsX", "twitteX", LocalDateTime.now()) | new BloggerEntry("name", "rss", "twitter") | false
            new Person("name", "rss", "twitter", LocalDateTime.now()) | new BloggerEntry("name", "rss", null)      | false
            new Person("name", "rss", "twitter", LocalDateTime.now()) | new BloggerEntry(null, "rss", "twitter")   | false
            new Person("name", "rss", "twitter", LocalDateTime.now()) | new BloggerEntry("name", null, "twitter")  | false
            new Person("name", "RSS", "twitter", LocalDateTime.now()) | new BloggerEntry("name", "rss", "twitter") | true
    }

    def "Should insert new Person for entry with new rss"() {
        given:
            String rss = "newRSS"
            BloggerEntry entry = new BloggerEntry("name", rss, "twitter")
            personRepository.findByRssIgnoreCase(rss) >> Optional.empty()
        when:
            BloggersDataUpdater.UpdateSummary summary = new BloggersDataUpdater.UpdateSummary(5)
            bloggersDataUpdater.updateSingleEntry(entry, summary)
        then:
            1 * personRepository.save(_ as Person)
            summary.createdEntries == 1
            summary.updatedEntries == 0
    }

    def "Should not update data if equal record already exists in DB"() {
        given:
            String rss = "existingRSS"
            BloggerEntry entry = new BloggerEntry("name", rss, "twitter")
            personRepository.findByRssIgnoreCase(rss) >> Optional.of(new Person(entry.name, entry.rss, entry.twitter, LocalDateTime.now()))
        when:
            BloggersDataUpdater.UpdateSummary summary = new BloggersDataUpdater.UpdateSummary(5)
            bloggersDataUpdater.updateSingleEntry(entry, summary)
        then:
            0 * personRepository.save(_ as Person)
            summary.createdEntries == 0
            summary.updatedEntries == 0
    }

    def "Should update data if data in entry data differs a bit from record in DB"() {
        given:
            String rss = "existingRSS"
            BloggerEntry entry = new BloggerEntry("name", rss, "twitter")
            personRepository.findByRssIgnoreCase(rss) >> Optional.of(new Person(entry.name, "oldRSS", entry.twitter, LocalDateTime.now()))
        when:
            BloggersDataUpdater.UpdateSummary summary = new BloggersDataUpdater.UpdateSummary(5)
            bloggersDataUpdater.updateSingleEntry(entry, summary)
        then:
            1 * personRepository.save(_ as Person)
            summary.createdEntries == 0
            summary.updatedEntries == 1
    }

}
