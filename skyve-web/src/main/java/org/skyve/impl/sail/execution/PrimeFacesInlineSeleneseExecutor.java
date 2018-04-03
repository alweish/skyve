package org.skyve.impl.sail.execution;

import java.util.List;

import javax.faces.component.UIComponent;

import org.primefaces.component.calendar.Calendar;
import org.primefaces.component.colorpicker.ColorPicker;
import org.primefaces.component.inputmask.InputMask;
import org.primefaces.component.inputtext.InputText;
import org.primefaces.component.inputtextarea.InputTextarea;
import org.primefaces.component.password.Password;
import org.primefaces.component.selectbooleancheckbox.SelectBooleanCheckbox;
import org.primefaces.component.selectonemenu.SelectOneMenu;
import org.primefaces.component.spinner.Spinner;
import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.module.ModuleImpl;
import org.skyve.impl.metadata.view.ViewImpl;
import org.skyve.impl.web.faces.pipeline.component.ComponentBuilder;
import org.skyve.impl.web.faces.pipeline.layout.LayoutBuilder;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Collection;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.query.DocumentQueryDefinition;
import org.skyve.metadata.repository.Repository;
import org.skyve.metadata.sail.language.Automation.TestStrategy;
import org.skyve.metadata.sail.language.Step;
import org.skyve.metadata.sail.language.step.TestFailure;
import org.skyve.metadata.sail.language.step.TestSuccess;
import org.skyve.metadata.sail.language.step.TestValue;
import org.skyve.metadata.sail.language.step.context.ClearContext;
import org.skyve.metadata.sail.language.step.context.PopContext;
import org.skyve.metadata.sail.language.step.context.PushEditContext;
import org.skyve.metadata.sail.language.step.context.PushListContext;
import org.skyve.metadata.sail.language.step.interaction.DataEnter;
import org.skyve.metadata.sail.language.step.interaction.TabSelect;
import org.skyve.metadata.sail.language.step.interaction.TestDataEnter;
import org.skyve.metadata.sail.language.step.interaction.actions.Action;
import org.skyve.metadata.sail.language.step.interaction.actions.Cancel;
import org.skyve.metadata.sail.language.step.interaction.actions.Delete;
import org.skyve.metadata.sail.language.step.interaction.actions.Ok;
import org.skyve.metadata.sail.language.step.interaction.actions.Remove;
import org.skyve.metadata.sail.language.step.interaction.actions.Save;
import org.skyve.metadata.sail.language.step.interaction.actions.ZoomOut;
import org.skyve.metadata.sail.language.step.interaction.grids.DataGridEdit;
import org.skyve.metadata.sail.language.step.interaction.grids.DataGridNew;
import org.skyve.metadata.sail.language.step.interaction.grids.DataGridRemove;
import org.skyve.metadata.sail.language.step.interaction.grids.DataGridSelect;
import org.skyve.metadata.sail.language.step.interaction.grids.DataGridZoom;
import org.skyve.metadata.sail.language.step.interaction.grids.ListGridNew;
import org.skyve.metadata.sail.language.step.interaction.grids.ListGridSelect;
import org.skyve.metadata.sail.language.step.interaction.grids.ListGridZoom;
import org.skyve.metadata.sail.language.step.interaction.lookup.LookupDescriptionAutoComplete;
import org.skyve.metadata.sail.language.step.interaction.lookup.LookupDescriptionEdit;
import org.skyve.metadata.sail.language.step.interaction.lookup.LookupDescriptionNew;
import org.skyve.metadata.sail.language.step.interaction.lookup.LookupDescriptionPick;
import org.skyve.metadata.sail.language.step.interaction.navigation.NavigateCalendar;
import org.skyve.metadata.sail.language.step.interaction.navigation.NavigateEdit;
import org.skyve.metadata.sail.language.step.interaction.navigation.NavigateLink;
import org.skyve.metadata.sail.language.step.interaction.navigation.NavigateList;
import org.skyve.metadata.sail.language.step.interaction.navigation.NavigateMap;
import org.skyve.metadata.sail.language.step.interaction.navigation.NavigateTree;
import org.skyve.metadata.user.User;
import org.skyve.metadata.view.View.ViewType;
import org.skyve.util.Binder.TargetMetaData;
import org.skyve.util.test.SkyveFixture.FixtureType;
import org.skyve.util.DataBuilder;

public class PrimeFacesInlineSeleneseExecutor extends InlineSeleneseExecutor<PrimeFacesAutomationContext> {
	private ComponentBuilder componentBuilder;
	private LayoutBuilder layoutBuilder;
	
	public PrimeFacesInlineSeleneseExecutor(ComponentBuilder componentBuilder,
												LayoutBuilder layoutBuilder) {
		this.componentBuilder = componentBuilder;
		this.layoutBuilder = layoutBuilder;
	}

	@Override
	public void executePushListContext(PushListContext push) {
		PrimeFacesAutomationContext newContext = new PrimeFacesAutomationContext();
		String moduleName = push.getModuleName();
		Customer c = getUser().getCustomer();
		Module m = c.getModule(moduleName);
		String documentName = push.getDocumentName();
		String queryName = push.getQueryName();
		String modelName = push.getModelName();
		
		if (queryName != null) {
			DocumentQueryDefinition q = m.getDocumentQuery(queryName);
			if (q == null) {
				q = m.getDocumentDefaultQuery(c, documentName);
			}
			m = q.getOwningModule();
			newContext.setModuleName(m.getName());
			newContext.setDocumentName(q.getDocumentName());
		}
		else if (documentName != null) {
			Document d = m.getDocument(c, documentName);
			if (modelName != null) {
				d = CORE.getRepository().getListModel(c, d, modelName, false).getDrivingDocument();
			}
			else {
				push.setQueryName(documentName);
			}
			newContext.setModuleName(d.getOwningModuleName());
			newContext.setDocumentName(d.getName());
		}
		else {
			throw new MetaDataException("NavigateList must have module and one of (query, document, document & mode)l");
		}

		newContext.setViewType(ViewType.list);
		newContext.setUxui(push.getUxui());
		newContext.setUserAgentType(push.getUserAgentType());
		push(newContext);
		newContext.generate(push, componentBuilder);
	}

	@Override
	public void executePushEditContext(PushEditContext push) {
		PrimeFacesAutomationContext newContext = new PrimeFacesAutomationContext();
		newContext.setModuleName(push.getModuleName());
		newContext.setDocumentName(push.getDocumentName());
		if (Boolean.TRUE.equals(push.getCreateView())) {
			newContext.setViewType(ViewType.create);
		}
		else {
			newContext.setViewType(ViewType.edit);
		}
		newContext.setUxui(push.getUxui());
		newContext.setUserAgentType(push.getUserAgentType());
		push(newContext);
		newContext.generate(push, componentBuilder, layoutBuilder);
	}
	
	@Override
	public void executeClearContext(ClearContext clear) {
		clear();
	}
	
	@Override
	public void executePopContext(PopContext pop) {
		pop();
	}
	
	@Override
	public void executeNavigateList(NavigateList list) {
		String moduleName = list.getModuleName();
		String documentName = list.getDocumentName();
		String queryName = list.getQueryName();
		String modelName = list.getModelName();

		PushListContext push = new PushListContext();
		push.setModuleName(moduleName);
		push.setDocumentName(documentName);
		push.setQueryName(queryName);
		push.setModelName(modelName);
		executePushListContext(push);

		if (queryName != null) {
			command("open", String.format("?a=l&m=%s&q=%s", moduleName, queryName));
		}
		else if (documentName != null) {
			if (modelName != null) {
				command("open", String.format("?a=l&m=%s&d=%s&q=%s", moduleName, documentName, modelName));
			}
			else {
				command("open", String.format("?a=l&m=%s&q=%s", moduleName, documentName));
			}
		}
	}

	@Override
	public void executeNavigateEdit(NavigateEdit edit) {
		String moduleName = edit.getModuleName();
		String documentName = edit.getDocumentName();
		
		PushEditContext push = new PushEditContext();
		push.setModuleName(moduleName);
		push.setDocumentName(documentName);
		executePushEditContext(push);

		String bizId = edit.getBizId();
		if (bizId == null) {
			comment(String.format("Edit new document [%s.%s] instance", moduleName, documentName));
			command("open", String.format(".?a=e&m=%s&d=%s", moduleName, documentName));
		}
		else {
			comment(String.format("Edit document [%s.%s] instance with bizId %s", moduleName, documentName, bizId));
			command("open", String.format(".?a=e&m=%s&d=%s&i=%s", moduleName, documentName, bizId));
		}
	}

	@Override
	public void executeNavigateTree(NavigateTree tree) {
		super.executeNavigateTree(tree); // determine driving document
		// TODO Auto-generated method stub
	}

	@Override
	public void executeNavigateMap(NavigateMap map) {
		super.executeNavigateMap(map); // determine driving document
		// TODO Auto-generated method stub
	}

	@Override
	public void executeNavigateCalendar(NavigateCalendar calendar) {
		super.executeNavigateCalendar(calendar); // determine driving document
		// TODO Auto-generated method stub
	}

	@Override
	public void executeNavigateLink(NavigateLink link) {
		super.executeNavigateLink(link); // null driving document
		// TODO Auto-generated method stub
	}

	@Override
	public void executeTabSelect(TabSelect tabSelect) {
		PrimeFacesAutomationContext context = peek();
		String identifier = tabSelect.getIdentifier(context);
		List<UIComponent> components = context.getFacesComponents(identifier);
		if (components == null) {
			throw new MetaDataException("<tabSelect /> with path [" + tabSelect.getTabPath() + "] is not valid or is not on the view.");
		}
		for (UIComponent component : components) {
			String clientId = ComponentCollector.clientId(component);
			comment(String.format("click tab [%s]", tabSelect.getTabPath()));
			command("click", String.format("//a[contains(@href, '#%s')]", clientId));
		}
	}

	@Override
	public void executeTestDataEnter(TestDataEnter testDataEnter) {
		PrimeFacesAutomationContext context = peek();
		String moduleName = context.getModuleName();
		String documentName = context.getDocumentName();
		User u = getUser();
		Customer c = u.getCustomer();
		Module m = c.getModule(moduleName);
		Document d = m.getDocument(c, documentName);
		Repository r = CORE.getRepository();
		
		Bean bean = null;
		String fixture = testDataEnter.getFixture();
		if (fixture == null) {
			bean = new DataBuilder(u).fixture(FixtureType.sail).build(d);
		}
		else {
			bean = new DataBuilder(u).fixture(fixture).build(d);
		}
		
        ViewImpl view = (ViewImpl) r.getView(context.getUxui(), c, d, context.getViewType().toString());
		TestDataEnterViewVisitor visitor = new TestDataEnterViewVisitor((CustomerImpl) c,
																			(ModuleImpl) m,
																			(DocumentImpl) d,
																			view,
																			bean);
		visitor.visit();
		
		for (Step steps : visitor.getScalarSteps()) {
			steps.execute(this);
		}
	}

	@Override
	public void executeDataEnter(DataEnter dataEnter) {
		PrimeFacesAutomationContext context = peek();
		String identifier = dataEnter.getIdentifier(context);
		List<UIComponent> components = context.getFacesComponents(identifier);
		if (components == null) {
			throw new MetaDataException("<DataEnter /> with binding [" + identifier + "] is not valid or is not on the view.");
		}
		for (UIComponent component : components) {
			String clientId = ComponentCollector.clientId(component);
			boolean text = (component instanceof InputText) || 
								(component instanceof InputTextarea) || 
								(component instanceof Password);
			boolean selectOne = (component instanceof SelectOneMenu);
			boolean masked = (component instanceof InputMask);
			boolean checkbox = (component instanceof SelectBooleanCheckbox);
			boolean _input = (component instanceof Spinner) || (component instanceof Calendar);
			
			// TODO implement colour picker testing
			if (component instanceof ColorPicker) {
				comment(String.format("Ignore colour picker %s (%s) for now", identifier, clientId));
				return;
			}
			
			// if exists and is not disabled
			comment(String.format("set %s (%s) if it exists and is not disabled", identifier, clientId));
			command("storeElementPresent", clientId, "present");
			command("if", "${present} == true");
			if (checkbox) {
				// dont need to check if checkbox is disabled coz we can still try to click it
				// check the value and only click if we need the other different value
				command("storeEval", String.format("window.SKYVE.getCheckboxValue('%s')", clientId), "checked");
				command("if", String.format("${checked} != %s", dataEnter.getValue()));
				command("click", String.format("%s_input", clientId));
				command("endIf");
			}
			else if (_input) {
				// determine editable as these are <input/>
				command("storeEditable", String.format("%s_input", clientId), "editable");
				command("if", "${editable} == true");
			}
			else if (selectOne) {
				// Look for prime faces disabled style
				command("storeCssCount", String.format("css=#%s.ui-state-disabled", clientId), "disabled");
				command("if", "${disabled} == false");
			}
			else {
				// determine editable as these are <input/>
				command("storeEditable", clientId, "editable");
				command("if", "${editable} == true");
			}
			
			if (text) {
				command("type", clientId, dataEnter.getValue());
			}
			else if (masked) { // need to send key strokes to masked fields
				command("click", clientId); // the click selects the existing expression for overtype
				command("sendKeys", clientId, dataEnter.getValue());
			}
			else if (_input) {
				command("type", String.format("%s_input", clientId), dataEnter.getValue());
			}
			else if (selectOne) {
				command("click", String.format("%s_label", clientId));
				// Value here should be an index in the drop down starting from 0
				command("click", String.format("%s_%s", clientId, dataEnter.getValue()));
			}
			if (! checkbox) { // endIf for disabled/editable test
				command("endIf");
			}
			command("endIf"); // endIf for present test
		}
	}

	private void button(Step button, String tagName, boolean ajax, boolean confirm, Boolean testSuccess) {
		PrimeFacesAutomationContext context = peek();
		String identifier = button.getIdentifier(context);
		List<UIComponent> components = context.getFacesComponents(identifier);
		if (components == null) {
			throw new MetaDataException(String.format("<%s /> is not on the view.", tagName));
		}
		for (UIComponent component : components) {
			String clientId = ComponentCollector.clientId(component);

			// if exists and is not disabled
			comment(String.format("click [%s] (%s) if it exists and is not disabled", tagName, clientId));
			command("storeElementPresent", clientId, "present");
			command("if", "${present} == true");
			// Look for prime faces disabled style
			command("storeCssCount", String.format("css=#%s.ui-state-disabled", clientId), "disabled");
			command("if", "${disabled} == false");
			if (ajax) {
				command("click", clientId);
				if (confirm) {
					command("click", "confirmOK");
				}
				command("waitForNotVisible", "busy");
			}
			else {
				if (confirm) {
					command("click", clientId);
					command("clickAndWait", "confirmOK");
				}
				else {
					command("clickAndWait", clientId);
				}
			}
			if (! Boolean.FALSE.equals(testSuccess)) { // true or null (defaults on)
				executeTestSuccess(new TestSuccess());
			}
			command("endIf");
			command("endIf");
		}
		
	}
	
	@Override
	public void executeOk(Ok ok) {
		button(ok, "ok", false, false, ok.getTestSuccess());
		pop();
	}

	@Override
	public void executeSave(Save save) {
		button(save, "save", true, false, save.getTestSuccess());
		Boolean createView = save.getCreateView(); // NB could be null
		if (Boolean.TRUE.equals(createView)) {
			PrimeFacesAutomationContext context = peek();
			context.setViewType(ViewType.create);
		}
		else if (Boolean.FALSE.equals(createView)) {
			PrimeFacesAutomationContext context = peek();
			context.setViewType(ViewType.edit);
		}
	}

	@Override
	public void executeCancel(Cancel cancel) {
		button(cancel, "cancel", false, false, cancel.getTestSuccess());
		pop();
	}

	@Override
	public void executeDelete(Delete delete) {
		button(delete, "delete", false, true, delete.getTestSuccess());
		pop();
	}

	@Override
	public void executeZoomOut(ZoomOut zoomOut) {
		button(zoomOut, "zoom out", false, false, zoomOut.getTestSuccess());
		pop();
	}

	@Override
	public void executeRemove(Remove remove) {
		button(remove, "remove", false, true, remove.getTestSuccess());
		pop();
	}

	@Override
	public void executeAction(Action action) {
		button(action, 
				action.getActionName(),
				true,
				Boolean.TRUE.equals(action.getConfirm()),
				action.getTestSuccess());
	}

	@Override
	public void executeLookupDescriptionAutoComplete(LookupDescriptionAutoComplete complete) {
		lookupDescription(complete, complete.getBinding(), null, complete.getSearch());
	}

	@Override
	public void executeLookupDescriptionPick(LookupDescriptionPick pick) {
		lookupDescription(pick, pick.getBinding(), pick.getRow(), null);
	}

	private void lookupDescription(Step step, String binding, Integer row, String search) {
		PrimeFacesAutomationContext context = peek();
		
		List<UIComponent> lookupComponents = context.getFacesComponents(binding);
		if (lookupComponents == null) {
			throw new MetaDataException(String.format("<%s /> with binding [%s] is not on the view.",
														step.getClass().getSimpleName(),
														binding));
		}
		for (UIComponent lookupComponent : lookupComponents) {
			String clientId = ComponentCollector.clientId(lookupComponent);
			if (row != null) {
				comment(String.format("Pick on row %d on lookup description [%s] (%s)", row, binding, clientId));
			}
			else {
				comment(String.format("Auto complete with search '%s' on lookup description [%s] (%s)", search, binding, clientId));
			}
			
			// lookup description is present
			command("storeElementPresent", clientId, "present");
			command("if", "${present} == true");
			// determine editable as these are <input/>
			command("storeEditable", String.format("%s_input", clientId), "editable");
			command("if", "${editable} == true");

			if (row != null) {
				// Click the drop down button
				command("clickAt", String.format("//span[@id='%s']/button", clientId), "10,10");
				// Wait for pick list drop down
				command("waitForVisible", String.format("//div[@id='%s_panel']", clientId));
				// Select the row
				command("click", String.format("//div[@id='%s_panel']/ul/li[%d]", clientId, Integer.valueOf(row.intValue() + 1)));
			}
			else {
				// Type in the input field (everything but the last char)
				command("type", String.format("%s_input", clientId), search.substring(0, search.length() - 1));
				command("sendKeys", String.format("%s_input", clientId), search.substring(search.length() - 1));

				// Wait for pick list drop down
				command("waitForVisible", String.format("//div[@id='%s_panel']", clientId));
				// Select the first row
				command("click", String.format("//div[@id='%s_panel']/ul/li", clientId));
			}
			
			command("endIf");
			command("endIf");
		}
	}

	@Override
	public void executeLookupDescriptionNew(LookupDescriptionNew nu) {
		// Nothing to do here as PF doesn't allow new off of lookup descriptions
	}

	@Override
	public void executeLookupDescriptionEdit(LookupDescriptionEdit edit) {
		// Nothing to do here as PF doesn't allow edit off of lookup descriptions
	}

	@Override
	public void executeDataGridNew(DataGridNew nu) {
		dataGridButton(nu, nu.getBinding(), null);
	}
	
	@Override
	public void executeDataGridZoom(DataGridZoom zoom) {
		dataGridButton(zoom, zoom.getBinding(), zoom.getRow());
	}
	
	@Override
	public void executeDataGridRemove(DataGridRemove remove) {
		dataGridButton(remove, remove.getBinding(), remove.getRow());
	}

	private void dataGridButton(Step step, String binding, Integer row) {
		PrimeFacesAutomationContext context = peek();
		String buttonIdentifier = step.getIdentifier(context);
		
		List<UIComponent> dataGridComponents = context.getFacesComponents(binding);
		if (dataGridComponents == null) {
			throw new MetaDataException(String.format("<%s /> with binding [%s] is not on the view.",
														(row != null) ? 
															((step instanceof DataGridZoom) ? "DataGridZoom" : "DataGridRemove") : 
															"DataGridNew",
														binding));
		}
		for (UIComponent dataGridComponent : dataGridComponents) {
			String dataGridClientId = ComponentCollector.clientId(dataGridComponent);
			if (row != null) {
				if (step instanceof DataGridZoom) {
					comment(String.format("Zoom on row %d on data grid [%s] (%s)", row, binding, dataGridClientId));
				}
				else {
					comment(String.format("Remove on row %d on data grid [%s] (%s)", row, binding, dataGridClientId));
				}
			}
			else {
				comment(String.format("New row on data grid [%s] (%s)", binding, dataGridClientId));
			}
			List<UIComponent> buttonComponents = context.getFacesComponents(buttonIdentifier);
			if (buttonComponents != null) { // button may not be shown
				for (UIComponent buttonComponent : buttonComponents) {
					String buttonClientId = (row != null) ?
												ComponentCollector.clientId(buttonComponent, row) :
												ComponentCollector.clientId(buttonComponent);
					if (buttonClientId.startsWith(dataGridClientId)) {
						// data grid is present
						command("storeElementPresent", dataGridClientId, "present");
						command("if", "${present} == true");
						// data grid button is present
						command("storeElementPresent", buttonClientId, "present");
						command("if", "${present} == true");
						// Look for prime faces disabled style on data grid button
						command("storeCssCount", String.format("css=#%s.ui-state-disabled", buttonClientId), "disabled");
						command("if", "${disabled} == false");
						// All good, continue with the button click
						if (step instanceof DataGridRemove) {
							command("click", ComponentCollector.clientId(buttonComponent, row));
							command("waitForNotVisible", "busy");
						}
						else {
							command("clickAndWait", buttonClientId);
						}
						command("endIf");
						command("endIf");
						command("endIf");
					}
				}
			}
		}

		// Determine the Document of the edit view to push
		Customer c = getUser().getCustomer();
		Module m = c.getModule(context.getModuleName());
		Document d = m.getDocument(c, context.getDocumentName());
		TargetMetaData target = BindUtil.getMetaDataForBinding(c, m, d, binding);
		String newDocumentName = ((Collection) target.getAttribute()).getDocumentName();
		d = m.getDocument(c, newDocumentName);
		String newModuleName = d.getOwningModuleName();
		
		// Push it
		PushEditContext push = new PushEditContext();
		push.setModuleName(newModuleName);
		push.setDocumentName(newDocumentName);
		push.execute(this);
	}

	@Override
	public void executeDataGridEdit(DataGridEdit edit) {
		// cannot edit a grid row in PF
	}

	@Override
	public void executeDataGridSelect(DataGridSelect select) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeListGridNew(ListGridNew nu) {
		listGridButton(nu, null);
		
		PrimeFacesAutomationContext context = peek();
		PushEditContext push = new PushEditContext();
		push.setModuleName(context.getModuleName());
		push.setDocumentName(context.getDocumentName());
		push.setCreateView(nu.getCreateView());
		push.execute(this);
	}

	@Override
	public void executeListGridZoom(ListGridZoom zoom) {
		listGridButton(zoom, zoom.getRow());
		
		PrimeFacesAutomationContext context = peek();
		PushEditContext push = new PushEditContext();
		push.setModuleName(context.getModuleName());
		push.setDocumentName(context.getDocumentName());
		push.execute(this);
	}

	@Override
	public void executeListGridSelect(ListGridSelect select) {
		listGridButton(select, select.getRow());
		
		// TODO only if there is no select event on the skyve edit view for embedded list grid
		PrimeFacesAutomationContext context = peek();
		PushEditContext push = new PushEditContext();
		push.setModuleName(context.getModuleName());
		push.setDocumentName(context.getDocumentName());
		push.execute(this);
	}

	// TODO handle embedded list grid from an edit view
	private void listGridButton(Step step, Integer row) {
		PrimeFacesAutomationContext context = peek();
		String buttonIdentifier = step.getIdentifier(context);
		String listGridIdentifier = buttonIdentifier.substring(0, buttonIdentifier.lastIndexOf('.'));
		
		List<UIComponent> listGridComponents = context.getFacesComponents(listGridIdentifier);
		if (listGridComponents == null) {
			throw new MetaDataException(String.format("<%s /> with identifier [%s] is not defined.",
															step.getClass().getSimpleName(),
															listGridIdentifier));
		}
		for (UIComponent listGridComponent : listGridComponents) {
			String listGridClientId = ComponentCollector.clientId(listGridComponent);
			if (row != null) {
				if (step instanceof ListGridZoom) {
					comment(String.format("Zoom on row %d on list grid [%s] (%s)", row, listGridIdentifier, listGridClientId));
				}
				else if (step instanceof ListGridSelect) {
					comment(String.format("Select on row %d on list grid [%s] (%s)", row, listGridIdentifier, listGridClientId));
				}
				else {
					comment(String.format("Delete on row %d on list grid [%s] (%s)", row, listGridIdentifier, listGridClientId));
				}
			}
			else {
				comment(String.format("New row on list grid [%s] (%s)", listGridIdentifier, listGridClientId));
			}
			List<UIComponent> buttonComponents = context.getFacesComponents(buttonIdentifier);
			if (buttonComponents != null) { // button may not be shown
				for (UIComponent buttonComponent : buttonComponents) {
					String buttonClientId = (row != null) ?
												ComponentCollector.clientId(buttonComponent, row) :
												ComponentCollector.clientId(buttonComponent);
					if (buttonClientId.startsWith(listGridClientId)) {
						// list grid is present
						command("storeElementPresent", listGridClientId, "present");
						command("if", "${present} == true");
						// list grid button is present
						command("storeElementPresent", buttonClientId, "present");
						command("if", "${present} == true");
						// Look for prime faces disabled style on list grid button
						command("storeCssCount", String.format("css=#%s.ui-state-disabled", buttonClientId), "disabled");
						command("if", "${disabled} == false");
						// All good, continue with the button click
						if (step instanceof ListGridSelect) {
							command("clickAndWait", String.format("//tr[%d]/td", row)); // ClientIdCollector.clientId(component, select.getRow()));
						}
						else {
							command("clickAndWait", buttonClientId);
						}
						command("endIf");
						command("endIf");
						command("endIf");
					}
				}
			}
		}
	}

	@Override
	public void executeTestValue(TestValue test) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void executeTestSuccess(TestSuccess test) {
		TestStrategy strategy = getTestStrategy();
		if (TestStrategy.Verify.equals(strategy)) {
			comment("Test Success");
			command("verifyElementNotPresent", "css=.ui-messages-error");
			command("verifyElementNotPresent", "css=.ui-message-error");
		}
		else if (TestStrategy.None.equals(strategy)) {
			// nothing to do
		}
		else { // null or Assert
			comment("Test Success");
			command("assertElementNotPresent", "css=.ui-messages-error");
			command("assertElementNotPresent", "css=.ui-message-error");
		}
	}
	
	@Override
	public void executeTestFailure(TestFailure test) {
		TestStrategy strategy = getTestStrategy();
		if (! TestStrategy.None.equals(strategy)) {
			String message = test.getMessage();
			if (message == null) {
				comment("Test Failure");
				if (TestStrategy.Verify.equals(strategy)) {
					command("verifyElementPresent", "css=.ui-messages-error");
				}
				else {
					command("assertElementPresent", "css=.ui-messages-error");
				}
			}
			else {
				comment(String.format("Test Failure with message '%s'", message));
				if (TestStrategy.Verify.equals(strategy)) {
					command("verifyElementPresent", "css=.ui-messages-error");
					command("verifyTextPresent", message);
				}
				else {
					command("assertElementPresent", "css=.ui-messages-error");
					command("assertTextPresent", message);
				}
			}
		}
	}
}
