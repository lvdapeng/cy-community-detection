package org.cytoscape.app.communitydetection.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cytoscape.app.communitydetection.util.AppUtils;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResultStatus;
import org.ndexbio.communitydetection.rest.model.CustomParameter;
import org.ndexbio.communitydetection.rest.model.Task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * REST API client for CD service. Implements GET, POST and DELETE.
 *
 */
public class CDRestClient {

	private final static String URI = "http://ddot.ucsd.edu/cd/communitydetection/v1";

	private final ObjectMapper mapper;
	private boolean isTaskCanceled;
	private Map<String, Map<String, Double>> resolutionParamMap;
	private List<CommunityDetectionAlgorithm> algorithms;

	private CDRestClient() {
		mapper = new ObjectMapper();
		isTaskCanceled = false;
		resolutionParamMap = new LinkedHashMap<String, Map<String, Double>>();
	}

	private static class SingletonHelper {
		private static final CDRestClient INSTANCE = new CDRestClient();
	}

	public static CDRestClient getInstance() {
		return SingletonHelper.INSTANCE;
	}

	/**
	 * Adds a key-value pair to resolutionParamMap. If key exists, the older value
	 * will be replaced.
	 */
	public void addToResolutionParamMap(String key, Map<String, Double> value) {
		resolutionParamMap.put(key, value);
	}

	/**
	 * Returns the value mapped to key. Returns null if key doesn't exist.
	 */
	public Map<String, Double> getResolutionParam(String key) {
		return resolutionParamMap.get(key);
	}

	/**
	 * Returns resolutionParamMap's keySet.
	 */
	public Set<String> getResolutionParamKeySet() {
		return resolutionParamMap.keySet();
	}

	public String postCDData(String algorithm, Boolean graphDirected, String data) throws Exception {

		CommunityDetectionRequest request = new CommunityDetectionRequest();
		Map<String, String> customParameters = new HashMap<String, String>();
		request.setAlgorithm(algorithm);
		request.setData(new TextNode(data));
		Map<String, List<CustomParameter>> resParams = getResolutionParameters();
		for (CommunityDetectionAlgorithm cdAlgo : getAlgorithms()) {
			if (cdAlgo.getName().equalsIgnoreCase(algorithm) && resolutionParamMap.containsKey(algorithm)) {
				for (CustomParameter param : resParams.get(algorithm)) {
					customParameters.put(param.getName(),
							Double.toString(getResolutionParam(algorithm).get(param.getName())));
					request.setCustomParameters(customParameters);
				}
			}
		}
		StringEntity body = new StringEntity(mapper.writeValueAsString(request));

		CloseableHttpClient client = getClient(10);
		HttpPost postRequest = new HttpPost(URI);
		postRequest.addHeader("accept", "application/json");
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.setEntity(body);

		HttpResponse httpPostResponse = null;
		for (int count = 0; count < 2; count++) {
			httpPostResponse = client.execute(postRequest);
			if (202 == httpPostResponse.getStatusLine().getStatusCode() || isTaskCanceled) {
				break;
			}
		}
		if (202 != httpPostResponse.getStatusLine().getStatusCode()) {
			throw new Exception("Error: " + httpPostResponse.getStatusLine().getStatusCode());
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpPostResponse.getEntity().getContent()));
		Task serviceTask = mapper.readValue(reader, Task.class);
		System.out.println("Task ID: " + serviceTask.getId());
		return serviceTask.getId();
	}

	public void deleteTask(String taskId) throws Exception {
		CloseableHttpClient client = getClient(10);
		HttpResponse deleteResponse = client.execute(new HttpDelete(URI + "/" + taskId));
		if (200 != deleteResponse.getStatusLine().getStatusCode()) {
			System.out.println("Could not delete task: " + taskId);
		}
	}

	public CommunityDetectionResult getCDResult(String taskId, Integer totalRuntime) throws Exception {
		CommunityDetectionResult cdResult = null;
		int waitTime = 1000;
		CloseableHttpClient client = getClient(totalRuntime);
		for (int count = 0; count < totalRuntime; count++) {
			Thread.sleep(waitTime);
			if (isTaskCanceled) {
				deleteTask(taskId);
				break;
			}
			HttpResponse httpGetResponse = client.execute(new HttpGet(URI + "/" + taskId));
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpGetResponse.getEntity().getContent()));
			cdResult = mapper.readValue(reader, CommunityDetectionResult.class);
			if (cdResult.getStatus().equals(CommunityDetectionResultStatus.COMPLETE_STATUS)) {
				break;
			}
			if (cdResult.getStatus().equals(CommunityDetectionResultStatus.FAILED_STATUS)) {
				throw new Exception("Error fetching the result!");
			}
		}
		if (cdResult != null
				&& !(cdResult.getStatus().equals(CommunityDetectionResultStatus.COMPLETE_STATUS) || isTaskCanceled)) {
			throw new Exception("Request timed out!");
		}
		return cdResult;
	}

	public CommunityDetectionResult getCDResult(String taskId, TaskMonitor taskMonitor, Float currentProgress,
			Float totalProgress, Integer totalRuntime) throws Exception {
		CommunityDetectionResult cdResult = null;
		int waitTime = 1000;
		CloseableHttpClient client = getClient(totalRuntime);
		for (int count = 0; count < totalRuntime; count++) {
			Thread.sleep(waitTime);
			if (isTaskCanceled) {
				deleteTask(taskId);
				break;
			}
			HttpResponse httpGetResponse = client.execute(new HttpGet(URI + "/" + taskId));
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpGetResponse.getEntity().getContent()));
			cdResult = mapper.readValue(reader, CommunityDetectionResult.class);
			if (cdResult.getStatus().equals(CommunityDetectionResultStatus.COMPLETE_STATUS)) {
				break;
			}
			if (cdResult.getStatus().equals(CommunityDetectionResultStatus.FAILED_STATUS)) {
				throw new Exception("Error fetching the result!");
			}
			float progressRatio = totalRuntime;
			taskMonitor.setProgress(currentProgress + (totalProgress * (float) (count + 1)) / progressRatio);
		}
		if (cdResult != null
				&& !(cdResult.getStatus().equals(CommunityDetectionResultStatus.COMPLETE_STATUS) || isTaskCanceled)) {
			throw new Exception("Request timed out!");
		}
		return cdResult;
	}

	public List<CommunityDetectionAlgorithm> getAlgorithms() throws Exception {
		if (algorithms != null) {
			return algorithms;
		}
		algorithms = new ArrayList<CommunityDetectionAlgorithm>();
		CloseableHttpClient client = getClient(10);
		HttpResponse httpGetResponse = client.execute(new HttpGet(URI + "/" + "algorithms"));
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpGetResponse.getEntity().getContent()));
		CommunityDetectionAlgorithms algos = mapper.readValue(reader, CommunityDetectionAlgorithms.class);
		for (CommunityDetectionAlgorithm algo : algos.getAlgorithms().values()) {
			algorithms.add(algo);
		}
		return algorithms;
	}

	public List<CommunityDetectionAlgorithm> getAlgorithmsByType(String inputType) throws Exception {
		List<CommunityDetectionAlgorithm> algos = new ArrayList<CommunityDetectionAlgorithm>();
		for (CommunityDetectionAlgorithm algo : getAlgorithms()) {
			if (algo.getInputDataFormat().equalsIgnoreCase(inputType)) {
				algos.add(algo);
			}
		}
		return algos;
	}

	public Map<String, List<CustomParameter>> getResolutionParameters() throws Exception {
		Map<String, List<CustomParameter>> paramMap = new LinkedHashMap<String, List<CustomParameter>>();
		for (CommunityDetectionAlgorithm algo : getAlgorithmsByType(AppUtils.CD_ALGORITHM_INPUT_TYPE)) {
			Set<CustomParameter> paramSet = algo.getCustomParameters();
			for (CustomParameter param : paramSet) {
				if (param.getValidationType() != null
						&& param.getValidationType().equals(CustomParameter.NUMBER_VALIDATION)) {
					List<CustomParameter> paramList;
					if (paramMap.containsKey(algo.getName())) {
						paramList = paramMap.get(algo.getName());
					} else {
						paramList = new ArrayList<CustomParameter>();
					}
					paramList.add(param);
					paramMap.put(algo.getName(), paramList);
				}
			}
		}
		return paramMap;
	}

	public void setTaskCanceled(boolean isCanceled) {
		isTaskCanceled = isCanceled;
	}

	private CloseableHttpClient getClient(int timeout) {
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
		return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
	}
}
