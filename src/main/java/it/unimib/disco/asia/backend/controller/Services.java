package it.unimib.disco.asia.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimib.disco.asia.backend.config.ConciliatorConfig;
import it.unimib.disco.asia.backend.model.Service;
import it.unimib.disco.asia.backend.response.Conciliator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@Lazy
public class Services {

    private final ConciliatorConfig conciliatorConfig;

    private static Map<String, Conciliator[]> services = new HashMap<>();
    static {
        Conciliator[] general = {
                new Conciliator(Service.WIKIFIER.getId())
        };
        Conciliator[] geo = {
                new Conciliator(Service.GEONAMES.getId()),
                new Conciliator(Service.GEOTARGETS.getId())
        };
        Conciliator[] category = {
                new Conciliator(Service.GOOGLECAT.getId())
        };

        services.put("general", general);
        services.put("geo", geo);
        services.put("category", category);
    }

    @Autowired
    public Services(ConciliatorConfig conciliatorConfig) throws IOException {
        this.conciliatorConfig = conciliatorConfig;
        services = this.services();
    }

    @RequestMapping(value = "services", produces = "application/json")
	public Map<String, Conciliator[]> services() throws IOException {

        for (Map.Entry<String, Conciliator[]> entry : services.entrySet()) {
            for (Conciliator c: entry.getValue()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(new URL(conciliatorConfig.getEndpoint() + c.getId()));

                c.setName(root.get("name").asText());
                c.setIdentifierSpace(root.get("identifierSpace").asText());
                c.setSchemaSpace(root.get("schemaSpace").asText());
                if (root.has("extend") && root.get("extend").has("propose_properties")) {
                    JsonNode proposeProperties = root.get("extend").get("propose_properties");
                    c.setProposePropertiesEndpoint(proposeProperties.get("service_url").asText() +
                            proposeProperties.get("service_path").asText());
                }
            }
        }

        return services;
	}

	public Conciliator[] getConciliators() {
        return services.values().stream().flatMap(Stream::of).toArray(Conciliator[]::new);
    }

    public Conciliator[] getConciliatorsByGroup(String groupId) {
        return services.get(groupId);
    }

    public Conciliator getConciliator(String conciliatorId) {
        return Stream.of(getConciliators()).filter(x -> x.getId().equalsIgnoreCase(conciliatorId)).findAny().get();
    }

}
