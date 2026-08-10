package nu.ndw.nls.routingmapmatcher.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PMapMapperTest {

    private final PMapMapper pMapMapper = new PMapMapper();

    @Mock
    private CustomModel customModel;

    @Test
    void createPropertyMapWithOptionalCustomModel_withCustomModel() {
        PMap result = pMapMapper.createPropertyMapWithOptionalCustomModel(customModel);

        assertThat(result.toMap())
                .containsEntry(CustomModel.KEY, customModel);
    }

    @Test
    void createPropertyMapWithOptionalCustomModel_withoutCustomModel() {
        PMap result = pMapMapper.createPropertyMapWithOptionalCustomModel(null);

        assertThat(result.toMap()).doesNotContainKey(CustomModel.KEY)
                .isEmpty();
    }

    @Test
    void mapCustomModelOrDefaultToShortestWeighting_withCustomModel() {
        PMap result = pMapMapper.mapCustomModelOrDefaultToShortestWeighting(customModel);

        assertThat(result.toMap())
                .containsEntry(CustomModel.KEY, customModel);
    }

    @Test
    void mapCustomModelOrDefaultToShortestWeighting_withoutCustomModel_defaultsToShortest() {
        PMap result = pMapMapper.mapCustomModelOrDefaultToShortestWeighting(null);

        assertThat(result.toMap())
                .containsEntry(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
    }
}