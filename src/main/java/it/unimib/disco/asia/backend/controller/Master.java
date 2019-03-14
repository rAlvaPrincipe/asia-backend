package it.unimib.disco.asia.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimib.disco.asia.backend.config.ConciliatorConfig;
import it.unimib.disco.asia.backend.response.Conciliator;
import it.unimib.disco.asia.backend.response.ConciliatorResult;
import it.unimib.disco.asia.backend.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Stream;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
public class Master {

	private final ConciliatorConfig conciliatorConfig;
	private final Conciliator[] services;
	private Map<String, List<ConciliatorResult>> map = new HashMap<>();

	@Autowired
	public Master(ConciliatorConfig conciliatorConfig, Services services) throws IOException {
		this.conciliatorConfig = conciliatorConfig;
		this.services = services.services().values().stream().flatMap(Stream::of).toArray(Conciliator[]::new);
	}


	@RequestMapping(value = "reconcile", produces = "application/json")
	public JsonNode reconcile ( @RequestParam (value = "queries") String queries,
			@RequestParam (value = "conciliator") String conciliator ) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(new URL(conciliatorConfig.getEndpoint() + conciliator + "?queries=" + URLEncoder.encode(queries, "UTF-8")));

		return root;
	}

	@RequestMapping(value = "extend", produces = "application/json")
	public JsonNode extend ( @RequestParam (value = "extend") String extend,
							  @RequestParam (value = "conciliator") String conciliator ) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(new URL(conciliatorConfig.getEndpoint() + conciliator + "?extend=" + URLEncoder.encode(extend, "UTF-8")));

		return root;
	}

	@RequestMapping(value = "propose_properties", produces = "application/json")
	public JsonNode proposeProperties ( @RequestParam (value = "type", required = false) String type,
							 @RequestParam (value = "limit", required = false) Integer limit,
							 @RequestParam (value = "conciliator") String conciliator ) throws Exception {

		if (limit == null) {
			limit = 10;
		}

		String parameters = "?limit=" + limit;
		if (type != null) {
			parameters += "&type=" + URLEncoder.encode(type, "UTF-8");
		}

		ObjectMapper mapper = new ObjectMapper();
		Conciliator c = Stream.of(services).filter(x -> x.getId().equalsIgnoreCase(conciliator)).findAny().get();
		return mapper.readTree(new URL(c.getProposePropertiesEndpoint() + parameters));

	}


	@RequestMapping(value = "suggest", produces = "application/json")
	public  Map<String, List<ConciliatorResult>> suggest ( @RequestParam (value = "queries") String queries,
			@RequestParam (value = "group") String group ) throws Exception {
		
		map.clear();

		for (Conciliator conciliator : services ) {
			query(queries, conciliator.getId());
		}

		return map;
	}


	private void query(String queries, String conciliator) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(new URL(conciliatorConfig.getEndpoint() + conciliator + "?queries=" + URLEncoder.encode(queries, "UTF-8")));
		Iterator<String> it = root.fieldNames();

		while(it.hasNext()) {
			String q = it.next();
			Result[] list = mapper.readValue(root.get(q).get("result").toString(), Result[].class);

			ConciliatorResult conciliatorResult = new ConciliatorResult();
			conciliatorResult.setConciliator(conciliator);
			conciliatorResult.setResult(list);

			List<ConciliatorResult> value = new ArrayList<>();
			if (map.containsKey(q))
				value = map.get(q);
			value.add(conciliatorResult);

			map.put(q, value);	
		}
	}

}
