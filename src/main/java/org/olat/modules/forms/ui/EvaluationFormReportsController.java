/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.forms.ui;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.modules.forms.EvaluationFormSession;
import org.olat.modules.forms.model.xml.Form;

/**
 * 
 * Initial date: 01.06.2018<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class EvaluationFormReportsController extends BasicController {
	
	private static final String CMD_PRINT = "report.print";
	private static final String CMD_EXPORT = "report.export";
	
	private Link printLink;
	private Link exportLink;
	
	private CloseableCalloutWindowController calloutCtrl;
	private EvaluationFormPrintSelectionController printSelectionCtrl;
	
	private final Form form;
	private final List<EvaluationFormSession> sessions;
	private final ReportHelper reportHelper;

	public EvaluationFormReportsController(UserRequest ureq, WindowControl wControl, Form form,
			List<EvaluationFormSession> sessions) {
		super(ureq, wControl);
		this.form = form;
		this.sessions = sessions;
		
		LegendNameGenerator legendNameGenerator = new SessionInformationLegendNameGenerator(sessions);
		this.reportHelper = ReportHelper.builder(getLocale())
				.withLegendNameGenrator(legendNameGenerator)
				.withColors()
				.build();

		VelocityContainer mainVC = createVelocityContainer("reports");

		printLink = LinkFactory.createButtonSmall(CMD_PRINT, mainVC, this);
		printLink.setIconLeftCSS("o_icon o_icon-fw o_icon_eva_print");

		exportLink = LinkFactory.createButtonSmall(CMD_EXPORT, mainVC, this);
		exportLink.setIconLeftCSS("o_icon o_icon-fw o_icon_eva_export");

		EvaluationFormReportSegmentsController segmentsController = new EvaluationFormReportSegmentsController(ureq,
				getWindowControl(), form, sessions, reportHelper);
		mainVC.put("segments", segmentsController.getInitialComponent());

		putInitialPanel(mainVC);
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source instanceof Link) {
			Link link = (Link) source;
			String cmd = link.getCommand();
			if (cmd.equals(CMD_PRINT)) {
				doOpenPrintSelection(ureq);
			} else if (cmd.equals(CMD_EXPORT)) {
				doExport(ureq);
			}
		}
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == printSelectionCtrl) {
			calloutCtrl.deactivate();
			cleanUp();
		}
		super.event(ureq,  source,  event);
	}
	
	@Override
	protected void doDispose() {
		removeAsListenerAndDispose(printSelectionCtrl);
		printSelectionCtrl = null;
	}

	private void cleanUp() {
		removeAsListenerAndDispose(calloutCtrl);
		calloutCtrl = null;
	}

	private void doOpenPrintSelection(UserRequest ureq) {
		if (printSelectionCtrl == null) {
			printSelectionCtrl = new EvaluationFormPrintSelectionController(ureq, getWindowControl(), form, sessions,
					reportHelper);
			listenTo(printSelectionCtrl);
		}

		removeAsListenerAndDispose(calloutCtrl);
		calloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
				printSelectionCtrl.getInitialComponent(), printLink, "", true, null);
		listenTo(calloutCtrl);
		calloutCtrl.activate();
	}

	private void doExport(UserRequest ureq) {
		String surveyName = "survey";
		EvaluationFormExcelExport export = new EvaluationFormExcelExport(form, sessions, reportHelper, surveyName);
		ureq.getDispatchResult().setResultingMediaResource(export.createMediaResource());
	}

}