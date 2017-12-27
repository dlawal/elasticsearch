/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.actions.ActionWrapper;
import org.elasticsearch.xpack.watcher.condition.ExecutableCondition;
import org.elasticsearch.xpack.watcher.input.ExecutableInput;
import org.elasticsearch.xpack.watcher.transform.ExecutableTransform;
import org.elasticsearch.xpack.watcher.trigger.Trigger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Watch implements ToXContentObject {

    public static final String INCLUDE_STATUS_KEY = "include_status";
    public static final String INDEX = ".watches";
    public static final String DOC_TYPE = "doc";

    private final String id;
    private final Trigger trigger;
    private final ExecutableInput input;
    private final ExecutableCondition condition;
    @Nullable private final ExecutableTransform transform;
    private final List<ActionWrapper> actions;
    @Nullable private final TimeValue throttlePeriod;
    @Nullable private final Map<String, Object> metadata;
    private final WatchStatus status;

    private transient long version = Versions.MATCH_ANY;

    public Watch(String id, Trigger trigger, ExecutableInput input, ExecutableCondition condition, @Nullable ExecutableTransform transform,
                 @Nullable TimeValue throttlePeriod, List<ActionWrapper> actions, @Nullable Map<String, Object> metadata,
                 WatchStatus status) {
        this.id = id;
        this.trigger = trigger;
        this.input = input;
        this.condition = condition;
        this.transform = transform;
        this.actions = actions;
        this.throttlePeriod = throttlePeriod;
        this.metadata = metadata;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public Trigger trigger() {
        return trigger;
    }

    public ExecutableInput input() { return input;}

    public ExecutableCondition condition() {
        return condition;
    }

    public ExecutableTransform transform() {
        return transform;
    }

    public TimeValue throttlePeriod() {
        return throttlePeriod;
    }

    public List<ActionWrapper> actions() {
        return actions;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public WatchStatus status() {
        return status;
    }

    public long version() {
        return version;
    }

    public void version(long version) {
        this.version = version;
    }

    /**
     * Sets the state of this watch to in/active
     *
     * @return  {@code true} if the status of this watch changed, {@code false} otherwise.
     */
    public boolean setState(boolean active, DateTime now) {
        return status.setActive(active, now);
    }

    /**
     * Acks this watch.
     *
     * @return  {@code true} if the status of this watch changed, {@code false} otherwise.
     */
    public boolean ack(DateTime now, String... actions) {
        return status.onAck(now, actions);
    }

    public boolean acked(String actionId) {
        ActionStatus actionStatus = status.actionStatus(actionId);
        return actionStatus.ackStatus().state() == ActionStatus.AckStatus.State.ACKED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Watch watch = (Watch) o;
        return watch.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(WatchField.TRIGGER.getPreferredName()).startObject().field(trigger.type(), trigger, params).endObject();
        builder.field(WatchField.INPUT.getPreferredName()).startObject().field(input.type(), input, params).endObject();
        builder.field(WatchField.CONDITION.getPreferredName()).startObject().field(condition.type(), condition, params).endObject();
        if (transform != null) {
            builder.field(WatchField.TRANSFORM.getPreferredName()).startObject().field(transform.type(), transform, params).endObject();
        }
        if (throttlePeriod != null) {
            builder.timeValueField(WatchField.THROTTLE_PERIOD.getPreferredName(),
                    WatchField.THROTTLE_PERIOD_HUMAN.getPreferredName(), throttlePeriod);
        }
        builder.startObject(WatchField.ACTIONS.getPreferredName());
        for (ActionWrapper action : actions) {
            builder.field(action.id(), action, params);
        }
        builder.endObject();
        if (metadata != null) {
            builder.field(WatchField.METADATA.getPreferredName(), metadata);
        }
        if (params.paramAsBoolean(INCLUDE_STATUS_KEY, false)) {
            builder.field(WatchField.STATUS.getPreferredName(), status, params);
        }
        builder.endObject();
        return builder;
    }

}
