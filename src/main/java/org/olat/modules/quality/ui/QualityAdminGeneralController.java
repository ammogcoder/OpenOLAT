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
package org.olat.modules.quality.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.modules.quality.QualityModule;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 04.07.2018<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class QualityAdminGeneralController extends FormBasicController {
	
	private static final String[] onKeys = new String[] { "on" };
	
	private MultipleSelectionElement enableEl;
	
	@Autowired
	private QualityModule qualityModule;
	
	public QualityAdminGeneralController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("admin.config.title");
		
		String[] onValues = new String[] { translate("on") };
		enableEl = uifactory.addCheckboxesHorizontal("admin.enabled", formLayout, onKeys, onValues);
		enableEl.addActionListener(FormEvent.ONCHANGE);
		if (qualityModule.isEnabled()) {
			enableEl.select(onKeys[0], true);
		}
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(enableEl == source) {
			qualityModule.setEnabled(enableEl.isAtLeastSelected(1));
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

}
