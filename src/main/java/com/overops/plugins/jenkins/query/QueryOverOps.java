/*
 * The MIT License
 *
 * Copyright (c) 2018, OverOps, Inc., Joe Offenberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.overops.plugins.jenkins.query;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.kohsuke.stapler.DataBoundConstructor;

import com.overops.common.api.util.ApiViewUtil;
import com.overops.plugins.jenkins.query.RegressionReportBuilder.RegressionReport;
import com.takipi.common.api.ApiClient;
import com.takipi.common.api.data.view.SummarizedView;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;

public class QueryOverOps extends Recorder implements SimpleBuildStep {
	
	private final int activeTimespan;
	private final int baselineTimespan;

	private final String criticalExceptionTypes;

	private final int minVolumeThreshold;
	private final double minErrorRateThreshold;

	private final double regressionDelta;
	private final double criticalRegressionDelta;

	private final int serverWait;
	private final boolean showResults;
	
	private final String serviceId;
	
	private final boolean markUnstable;

	
	@DataBoundConstructor
	public QueryOverOps(String applicationName, String deploymentName,
			int activeTimespan, int baselineTimespan,
			String criticalExceptionTypes,
			int minVolumeThreshold, double minErrorRateThreshold, 
			double regressionDelta, double criticalRegressionDelta,
			boolean markUnstable, boolean showResults, String serviceId,
			int serverWait) {
			
		
		this.criticalExceptionTypes = criticalExceptionTypes;

		this.activeTimespan = activeTimespan;
		this.baselineTimespan = baselineTimespan;

		this.minErrorRateThreshold = minErrorRateThreshold;
		this.minVolumeThreshold = minVolumeThreshold;

		this.regressionDelta = regressionDelta;
		this.criticalRegressionDelta = criticalRegressionDelta;

		this.serviceId = serviceId;
		
		this.serverWait = serverWait;
		this.showResults = showResults;
		this.markUnstable = markUnstable;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {

		String apiHost = getDescriptor().getOverOpsURL();
		String apiKey = Secret.toString(getDescriptor().getOverOpsAPIKey());
		
		String serviceId;
		
		if ((this.serviceId != null) && (!this.serviceId.isEmpty()))
		{
			serviceId = this.serviceId;
		}
		else
		{
			serviceId = getDescriptor().getOverOpsSID();
		}
		
		PrintStream printStream;
		
		if (showResults) {
			printStream = listener.getLogger();
		} else {
			printStream = null;	
		}
		
		ApiClient apiClient = ApiClient.newBuilder().setHostname(apiHost).setApiKey(apiKey).build();
		
		SummarizedView allEventsView = ApiViewUtil.getServiceViewByName(apiClient, serviceId, "All Events");

		if (allEventsView == null) {
			throw new IllegalStateException("Could not acquire ID for All events view");
		}
		
		if (serverWait > 0) {
			 
			if (showResults) {
				printStream.println("Waiting " + serverWait + " seconds for code analysis to complete");
			}
			
			TimeUnit.SECONDS.sleep(serverWait);
		}
		
		RegressionReport report = RegressionReportBuilder.execute(apiClient, serviceId, 
			allEventsView.id, activeTimespan, baselineTimespan, criticalExceptionTypes, 
			minVolumeThreshold, minErrorRateThreshold, regressionDelta, criticalRegressionDelta,
			printStream);
		
		OverOpsBuildAction buildAction = new OverOpsBuildAction(report, run);
		run.addAction(buildAction);
 
		if ((markUnstable) && (report.getUnstable())) {
			run.setResult(Result.UNSTABLE);
		}
	}
}
