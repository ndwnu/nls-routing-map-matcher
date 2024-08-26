package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
public class SinglePointMapMatcherWithBearingIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private static final double SNAPPED_RELIABILITY = 58.5059250048517;
    private static final double BEARING_RELIABILITY = 7.956622176353855;
    @Autowired
    private SinglePointMapMatcherFactory singlePointMapMatcherFactory;
    @Autowired
    private GeometryFactoryWgs84 geometryFactoryWgs84;
    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    private void setupNetwork() {
        singlePointMapMatcher = singlePointMapMatcherFactory.createMapMatcher(
                TestNetworkProvider.getTestNetworkFromFile(LINKS_RESOURCE), CAR);
    }

    @BeforeAll
    void beforeAll() {
        setupNetwork();
    }

    @Test
    void match_ok_withBearingFilter() {
        SinglePointLocation request = SinglePointLocation.builder()
                .id(1)
                .point(createPoint(5.426747, 52.176663))
                .cutoffDistance(10.0)
                .bearingFilter(new BearingFilter(135, 5))
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);

        assertThat(result, is(SinglePointMatch.builder()
                .id(1)
                .status(MatchStatus.MATCH)
                .reliability(BEARING_RELIABILITY)
                .candidateMatches(List.of(SinglePointMatch.CandidateMatch.builder()
                        .matchedLinkId(3667044)
                        .snappedPoint(createPoint(5.4267844367147156, 52.176683703033994))
                        .fraction(0.7955196672208802)
                        .distance(3.4374374525547)
                        .bearing(137.88345016490496)
                        .reliability(BEARING_RELIABILITY)
                        .build()))
                .build()));
    }

    @Test
    void match_ok_withBearingFilterAndSnappedPointAtEnd() {
        Point startPoint = createPoint(5.424289, 52.177873);
        SinglePointLocation request = SinglePointLocation.builder()
                .id(1)
                .point(startPoint)
                .cutoffDistance(25.0)
                .bearingFilter(new BearingFilter(160, 5))
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result, is(SinglePointMatch.builder()
                .id(1)
                .status(MatchStatus.MATCH)
                .reliability(SNAPPED_RELIABILITY)
                .candidateMatches(List.of(
                        SinglePointMatch.CandidateMatch.builder()
                                .matchedLinkId(3667015)
                                .snappedPoint(createPoint(5.424366071968406, 52.177889266140795))
                                .fraction(0.44795801658556095)
                                .distance(5.557781033819523)
                                .bearing(160.9631475429935)
                                .reliability(SNAPPED_RELIABILITY)
                                .build(),
                        SinglePointMatch.CandidateMatch.builder()
                                .matchedLinkId(3667014)
                                .snappedPoint(createPoint(5.424268, 52.178064))
                                .fraction(1)
                                .distance(21.286446542354614)
                                .bearing(161.06168302121978)
                                .reliability(0)
                                .build()))
                .build()));
    }

    private Point createPoint(double x, double y) {
        return geometryFactoryWgs84.createPoint(new Coordinate(x, y));
    }
}
