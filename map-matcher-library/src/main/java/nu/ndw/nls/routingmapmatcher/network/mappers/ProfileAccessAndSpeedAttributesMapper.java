package nu.ndw.nls.routingmapmatcher.network.mappers;

import com.graphhopper.config.Profile;
import java.util.List;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.network.model.ProfileAccessAndSpeedAttributes;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileAccessAndSpeedAttributesMapper {

    private final SpeedAndAccessAttributeMapper speedAndAccessAttributeMapper;

    public ProfileAccessAndSpeedAttributes map(List<Profile> profiles) {
        List<String> accessAttributes = profiles.stream()
                .map(speedAndAccessAttributeMapper::mapToAccessAttribute)
                .toList();
        List<String> speedAttributes = profiles.stream()
                .map(speedAndAccessAttributeMapper::mapToSpeedAttribute)
                .toList();
        return ProfileAccessAndSpeedAttributes
                .builder()
                .accessAttributes(accessAttributes)
                .speedAttributes(speedAttributes)
                .build();
    }

}
