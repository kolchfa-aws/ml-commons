/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.rest;

import static org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken;
import static org.opensearch.ml.plugin.MachineLearningPlugin.ML_BASE_URI;
import static org.opensearch.ml.utils.RestActionUtils.PARAMETER_LOAD_MODEL;
import static org.opensearch.ml.utils.RestActionUtils.PARAMETER_MODEL_ID;
import static org.opensearch.ml.utils.RestActionUtils.PARAMETER_VERSION;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.ml.common.transport.upload.MLUploadInput;
import org.opensearch.ml.common.transport.upload.MLUploadModelAction;
import org.opensearch.ml.common.transport.upload.MLUploadModelRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class RestMLUploadModelAction extends BaseRestHandler {
    private static final String ML_UPLOAD_MODEL_ACTION = "ml_upload_model_action";

    /**
     * Constructor
     */
    public RestMLUploadModelAction() {}

    @Override
    public String getName() {
        return ML_UPLOAD_MODEL_ACTION;
    }

    @Override
    public List<Route> routes() {
        return ImmutableList
            .of(
                new Route(RestRequest.Method.POST, String.format(Locale.ROOT, "%s/models/_upload", ML_BASE_URI)),
                new Route(
                    RestRequest.Method.POST,
                    String.format(Locale.ROOT, "%s/models/{%s}/{%s}/_upload", ML_BASE_URI, PARAMETER_MODEL_ID, PARAMETER_VERSION)
                )
            );
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        MLUploadModelRequest mlUploadModelRequest = getRequest(request);
        return channel -> client.execute(MLUploadModelAction.INSTANCE, mlUploadModelRequest, new RestToXContentListener<>(channel));
    }

    /**
     * Creates a MLTrainingTaskRequest from a RestRequest
     *
     * @param request RestRequest
     * @return MLTrainingTaskRequest
     */
    @VisibleForTesting
    MLUploadModelRequest getRequest(RestRequest request) throws IOException {
        String modelName = request.param(PARAMETER_MODEL_ID);
        String version = request.param(PARAMETER_VERSION);
        boolean loadModel = request.paramAsBoolean(PARAMETER_LOAD_MODEL, false);
        if (modelName != null && !request.hasContent()) {
            MLUploadInput mlInput = MLUploadInput.builder().loadModel(loadModel).modelName(modelName).version(version).build();
            return new MLUploadModelRequest(mlInput);
        }

        XContentParser parser = request.contentParser();
        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);
        MLUploadInput mlInput = modelName == null
            ? MLUploadInput.parse(parser, loadModel)
            : MLUploadInput.parse(parser, modelName, version, loadModel);
        return new MLUploadModelRequest(mlInput);
    }
}