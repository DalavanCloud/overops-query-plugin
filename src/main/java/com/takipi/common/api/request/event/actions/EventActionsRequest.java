package com.takipi.common.api.request.event.actions;

import java.io.UnsupportedEncodingException;

import com.takipi.common.api.request.ServiceRequest;
import com.takipi.common.api.request.intf.ApiGetRequest;
import com.takipi.common.api.result.event.actions.EventsActionsResult;
import com.takipi.common.api.util.ValidationUtil;

public class EventActionsRequest extends ServiceRequest implements ApiGetRequest<EventsActionsResult> {

	private final String eventId;

	EventActionsRequest(String serviceId, String eventId) {
		super(serviceId);
		this.eventId = eventId;
	}

	@Override
	public String urlPath() {
		return baseUrlPath() + "/events/" + eventId + "/actions";
	}
	
	@Override
	public Class<EventsActionsResult> resultClass() {
		return EventsActionsResult.class;
	}
	
	@Override
	public String[] getParams() throws UnsupportedEncodingException {
		return null;
	}
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder extends ServiceRequest.Builder {
		private String eventId;

		Builder() {

		}

		@Override
		public Builder setServiceId(String serviceId) {
			super.setServiceId(serviceId);

			return this;
		}

		public Builder setEventId(String eventId) {
			this.eventId = eventId;

			return this;
		}

		@Override
		protected void validate() {
			super.validate();

			if (!ValidationUtil.isLegalEventId(eventId)) {
				throw new IllegalArgumentException("Illegal event id - " + eventId);
			}
		}

		public EventActionsRequest build() {
			validate();

			return new EventActionsRequest(serviceId, eventId);
		}
	}
}
