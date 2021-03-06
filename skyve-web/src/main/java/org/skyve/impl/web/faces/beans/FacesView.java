package org.skyve.impl.web.faces.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.SelectEvent;
import org.skyve.domain.Bean;
import org.skyve.domain.ChildBean;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.metadata.view.widget.bound.FilterParameterImpl;
import org.skyve.impl.util.UtilImpl;
import org.skyve.impl.web.AbstractWebContext;
import org.skyve.impl.web.DynamicImageServlet;
import org.skyve.impl.web.UserAgentType;
import org.skyve.impl.web.faces.FacesAction;
import org.skyve.impl.web.faces.FacesUtil;
import org.skyve.impl.web.faces.actions.ActionUtil;
import org.skyve.impl.web.faces.actions.AddAction;
import org.skyve.impl.web.faces.actions.DeleteAction;
import org.skyve.impl.web.faces.actions.ExecuteActionAction;
import org.skyve.impl.web.faces.actions.GetBeansAction;
import org.skyve.impl.web.faces.actions.GetContentFileNameAction;
import org.skyve.impl.web.faces.actions.GetContentURLAction;
import org.skyve.impl.web.faces.actions.GetSelectItemsAction;
import org.skyve.impl.web.faces.actions.PreRenderAction;
import org.skyve.impl.web.faces.actions.RemoveAction;
import org.skyve.impl.web.faces.actions.RerenderAction;
import org.skyve.impl.web.faces.actions.SaveAction;
import org.skyve.impl.web.faces.actions.SetTitleAction;
import org.skyve.impl.web.faces.actions.ZoomInAction;
import org.skyve.impl.web.faces.actions.ZoomOutAction;
import org.skyve.impl.web.faces.models.BeanMapAdapter;
import org.skyve.impl.web.faces.models.SkyveDualListModelMap;
import org.skyve.impl.web.faces.models.SkyveLazyDataModel;
import org.skyve.impl.web.faces.pipeline.ResponsiveFormGrid;
import org.skyve.impl.web.faces.pipeline.component.ComponentBuilder;
import org.skyve.metadata.FilterOperator;
import org.skyve.metadata.router.UxUi;
import org.skyve.metadata.view.widget.bound.FilterParameter;
import org.skyve.util.Util;

@ViewScoped
@ManagedBean(name = "skyve")
public class FacesView<T extends Bean> extends Harness {
	private static final long serialVersionUID = 3331890232012703780L;

	// NB whatever state is added here needs to be handled by hydrate/dehydrate

	// This is set from a request attribute (the attribute is set in home.jsp)
	// NB This should be set once on post construct of the bean and it persists during all ajax requests.
	// NNB hydrate/dehydrate does not clear/set this property
	private UxUi uxui;
	// This is set from a request attribute (the attribute is set in home.jsp)
	// NB This should be set once on post construct of the bean and it persists during all ajax requests.
	// NNB hydrate/dehydrate does not clear/set this property
	private UserAgentType userAgentType;
	// The view binding - where we are zoomed into within the conversation bean.
	// This could be the same as zoom in binding or it could be deeper.
	private String viewBinding;
	// The zoomed in binding of the list (this could be compound)
	// This could be the same as the view binding or it could be shallower.
	// There is one entry per zoom in.
	private Stack<String> zoomInBindings = new Stack<>();
	// The page title
	private String title;
	private AbstractWebContext webContext;
	// The bean currently under edit (for the view binding)
	private BeanMapAdapter<T> currentBean = null;
	// A stack of referring urls set as we edit beans from a list grid
	private Stack<String> history = new Stack<>();
	private Map<String, SkyveLazyDataModel> lazyDataModels = new TreeMap<>();
 	private SkyveDualListModelMap dualListModels = new SkyveDualListModelMap(this);
	private Map<String, List<BeanMapAdapter<Bean>>> beans = new TreeMap<>();

	@PostConstruct
	protected void postConstruct() {
		this.uxui = (UxUi) FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get(AbstractWebContext.UXUI);
		this.userAgentType = (UserAgentType) FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get(FacesUtil.USER_AGENT_TYPE_KEY);
	}
	
	public void preRender() {
		FacesContext fc = FacesContext.getCurrentInstance();
		if (! fc.isPostback()) {
			new PreRenderAction<>(this).execute();
		}
		else if (UtilImpl.FACES_TRACE) {
			UtilImpl.LOGGER.info("FacesView - POSTPACK a=" + getWebActionParameter() + 
									" : m=" + getBizModuleParameter() + 
									" : d=" + getBizDocumentParameter() + 
									" : q=" + getQueryNameParameter() + 
									" : i=" + getBizIdParameter());
		}
	}

 	public UxUi getUxUi() {
		return uxui;
	}
	public void setUxUi(UxUi uxui) {
		this.uxui = uxui;
		FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(AbstractWebContext.UXUI, uxui);
	}

 	public UserAgentType getUserAgentType() {
		return userAgentType;
	}
	public void setUserAgentType(UserAgentType userAgentType) {
		this.userAgentType = userAgentType;
		FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(FacesUtil.USER_AGENT_TYPE_KEY, userAgentType);
	}

	public String getViewBinding() {
		return viewBinding;
	}
	public void setViewBinding(String viewBinding) {
		this.viewBinding = viewBinding;
	}

	public Stack<String> getZoomInBindings() {
		return zoomInBindings;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Stack<String> getHistory() {
		return history;
	}
	
	// The edited bean
	@SuppressWarnings("unchecked")
	public T getBean() {
        return (T) ((webContext == null) ? null : webContext.getCurrentBean());
    }
	@SuppressWarnings("unchecked")
	public void setBean(T bean)
	throws Exception {
		if (webContext != null) {
			webContext.setCurrentBean(bean);
		}
		currentBean = new BeanMapAdapter<>((T) ActionUtil.getTargetBeanForViewAndCollectionBinding(this, null, null));
	}

	public BeanMapAdapter<T> getCurrentBean() {
		return currentBean;
	}

	private long id = 0;
	public String nextId() {
		return new StringBuilder(10).append('s').append(id++).toString();
	}

	public AbstractWebContext getWebContext() {
		return webContext;
	}
	public void setWebContext(AbstractWebContext webContext) {
		this.webContext = webContext;
	}

	public void ok() {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - ok");
		new SaveAction<>(this, true).execute();
		
		FacesContext c = FacesContext.getCurrentInstance();
		if (c.getMessageList().isEmpty()) {
			try {
				c.getExternalContext().redirect(history.pop());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void save() {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - save");
		new SaveAction<>(this, false).execute();
		new SetTitleAction(this).execute();
		
		FacesContext c = FacesContext.getCurrentInstance();
		if (c.getMessageList().isEmpty()) {
			c.addMessage(null, new FacesMessage("Saved", "Any changes have been saved"));
		}
	}
	
	public void cancel() {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - cancel");
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect(history.pop());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void delete() {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - delete");
		new DeleteAction(this).execute();

		FacesContext c = FacesContext.getCurrentInstance();
		if (c.getMessageList().isEmpty()) {
			try {
				c.getExternalContext().redirect(history.pop());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// This corresponds to the lower case action name used in data grid generation (there is already edit())
	public void navigate(String listBinding, String bizId) {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - zoom in to " + listBinding + '.' + bizId);
		new ZoomInAction(this, listBinding, bizId).execute();
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - view binding now " + viewBinding);
	}
	
	// for navigate-on-select in data grids
	public void navigate(SelectEvent evt) {
		@SuppressWarnings("unchecked")
		String bizId = ((BeanMapAdapter<Bean>) evt.getObject()).getBean().getBizId();
		String listBinding = ((DataTable) evt.getComponent()).getVar();
		// change list var back to list binding - '_' to '.' and remove "Row" from the end.
		navigate(BindUtil.unsanitiseBinding(listBinding).substring(0, listBinding.length() - 3), bizId);
	}
	
	public void add(String listBinding, boolean inline) {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - add to " + listBinding + (inline ? " inline" : " with zoom"));
		new AddAction(this, listBinding, inline).execute();
		if (inline && UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - view binding now " + viewBinding);
	}
	
	public void zoomout() {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - zoomout");
		new ZoomOutAction<>(this).execute();
	}

	/**
	 * This method only removes elements from collections, it doesn't null out associations.
	 * removedHandlerActionNames uses "true/false" to indicate rerender action with/without client validation.
	 */
	public void remove(String listBinding, String bizId, List<String> removedHandlerActionNames) {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - remove " + viewBinding);
		new RemoveAction(this, listBinding, bizId, removedHandlerActionNames).execute();
	}

	public void action(String actionName, String listBinding, String bizId) {
		new ExecuteActionAction<>(this, 
									actionName, 
									UtilImpl.processStringValue(listBinding),
									UtilImpl.processStringValue(bizId)).execute();
		new SetTitleAction(this).execute();
	}

	public void rerender(String source, boolean validate) {
		new RerenderAction<>(this, source, validate).execute();
		new SetTitleAction(this).execute();
	}
	
	/**
	 * Set the selected row bizId and fire an action or rerender.
	 * 
	 * if actionName is null - do nothing - just set the selected row.
	 * else if actionName is "true" - rerender with validation.
	 * else if actionName is "false" - rerender with no validation.
	 * else run the action.
	 */
	public void selectGridRow(SelectEvent evt) {
		UIComponent component = evt.getComponent();
		Map<String, Object> attributes = component.getAttributes();
		String selectedIdBinding = (String) attributes.get("selectedIdBinding");
		String actionName = (String) attributes.get("actionName");

		@SuppressWarnings("unchecked")
		String bizId = ((BeanMapAdapter<Bean>) evt.getObject()).getBean().getBizId();
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - SET "+ selectedIdBinding + " to " + bizId);
		new FacesAction<Void>() {
			@Override
			public Void callback() throws Exception {
				BindUtil.set(getCurrentBean().getBean(), selectedIdBinding, bizId);
				return null;
			}
		}.execute();

		if (actionName != null) {
			if (Boolean.TRUE.toString().equals(actionName) || Boolean.FALSE.toString().equals(actionName)) {
				String source = (String) attributes.get("source");
				rerender(source, Boolean.parseBoolean(actionName));
			}
			else {
				action(actionName, null, null);
			}
		}
	}
	
	/**
	 * Ajax call from DataTable (data grid implementation) when rows are drag reordered in the UI.
	 * @param event
	 */
	public void onRowReorder(ReorderEvent event) {
		if (event != null) {
			final String collectionBinding = (String) event.getComponent().getAttributes().get(ComponentBuilder.COLLECTION_BINDING_ATTRIBUTE_KEY);
			if (collectionBinding != null) {
				@SuppressWarnings("unchecked")
				final List<Bean> list = (List<Bean>) BindUtil.get(getCurrentBean().getBean(), collectionBinding);
				list.add(event.getToIndex(), list.remove(event.getFromIndex()));

				for (int i = 0, l = list.size(); i < l; i++) {
					Bean element = list.get(i);
					if (element instanceof ChildBean<?>) {
						((ChildBean<?>) element).setBizOrdinal(Integer.valueOf(i));
					}
				}
			}
		}
	}

	public SkyveLazyDataModel getLazyDataModel(String moduleName, 
												String documentName, 
												String queryName,
												String modelName,
												List<List<String>> filterCriteria) {
		String key = null;
		if ((moduleName != null) && (queryName != null)) {
			key = String.format("%s.%s", moduleName, queryName);
		}
		else if ((moduleName != null) && (documentName != null)) {
			if (modelName != null) {
				key = String.format("%s.%s.%s", moduleName, documentName, modelName);
			}
			else {
				key = String.format("%s.%s", moduleName, documentName);
			}
		}
		SkyveLazyDataModel result = lazyDataModels.get(key);

		if (result == null) {
			// Collect the filter parameters from the criteria sent
			List<FilterParameter> filterParameters = null;
			if (filterCriteria != null) {
				filterParameters = new ArrayList<>(filterCriteria.size());
				for (List<String> filterCriterium : filterCriteria) {
					FilterParameterImpl param = new FilterParameterImpl();
					param.setName(filterCriterium.get(0));
					param.setOperator(FilterOperator.valueOf(filterCriterium.get(1)));
					param.setValue(filterCriterium.get(2));
					filterParameters.add(param);
				}
			}
			
			result = new SkyveLazyDataModel(this, moduleName, documentName, queryName, modelName, filterParameters);
			lazyDataModels.put(key, result);
		}
 		
		return result;
	}
	
	// Note - this is also called from EL in ListGrid tag
 	public List<BeanMapAdapter<Bean>> getBeans(final String bizModule, 
												final String queryName,
												final List<FilterParameter> parameters) {
 		List<BeanMapAdapter<Bean>> result = null;
 		
 		// these are ultimately web parameters that may not be present in the request
 		if ((queryName == null) || queryName.isEmpty()) {
 			result = new ArrayList<>();
 		}
 		else {
	 		StringBuilder key = new StringBuilder(64).append(bizModule).append('.').append(queryName);
	 		if (parameters != null) {
	 			for (FilterParameter parameter : parameters) {
	 				String valueOrBinding = parameter.getValue();
	 				if (valueOrBinding == null) {
	 					valueOrBinding = parameter.getBinding();
	 				}
	 				key.append('.').append(parameter.getName()).append(parameter.getOperator()).append(valueOrBinding);
	 			}
	 		}
	 		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - LIST KEY = " + key);
			result = beans.get(key.toString());
			if (result == null) {
				result = new GetBeansAction(this, bizModule, queryName, parameters).execute();
				beans.put(key.toString(), result);
			}
 		}
 		
		return result;
	}

	public SkyveDualListModelMap getDualListModels() {
		return dualListModels;
 	}

	// /skyve/download?_n=<action>&_doc=<module.document>&_c=<webId>&_ctim=<millis> and optionally &_b=<view binding>
	public String getDownloadUrl(String downloadActionName, String moduleName, String documentName) {
		StringBuilder result = new StringBuilder(128);
		result.append(Util.getSkyveContextUrl()).append("/download?");
		result.append(AbstractWebContext.RESOURCE_FILE_NAME).append('=').append(downloadActionName);
		result.append('&').append(AbstractWebContext.DOCUMENT_NAME).append('=');
		result.append(moduleName).append('.').append(documentName);
		result.append('&').append(AbstractWebContext.CONTEXT_NAME).append('=').append(webContext.getWebId());
		if (viewBinding != null) {
			result.append('&').append(AbstractWebContext.BINDING_NAME).append('=').append(viewBinding);
		}
		result.append('&').append(AbstractWebContext.CURRENT_TIME_IN_MILLIS).append('=').append(System.currentTimeMillis());
		return result.toString();
	}

	// /skyve/contentUpload.xhtml?_n=<binding>&_c=<webId> and optionally &_b=<view binding>
	public String getContentUploadUrl(String sanitisedBinding) {
		StringBuilder result = new StringBuilder(128);
		result.append(Util.getSkyveContextUrl()).append("/contentUpload.xhtml?");
		result.append(AbstractWebContext.RESOURCE_FILE_NAME).append('=').append(sanitisedBinding);
		result.append('&').append(AbstractWebContext.CONTEXT_NAME).append('=').append(webContext.getWebId());
		if (viewBinding != null) {
			result.append('&').append(AbstractWebContext.BINDING_NAME).append('=').append(viewBinding);
		}
		return result.toString();
	}
	
	public String getContentUrl(final String binding, final boolean image) {
 		return new GetContentURLAction(getCurrentBean().getBean(), binding, image).execute();
 	}
 	
 	public String getContentFileName(final String binding) {
 		return new GetContentFileNameAction(getCurrentBean().getBean(), binding).execute();
 	}
 	
 	public String getDynamicImageUrl(String name, 
 										String moduleName,
 										String documentName,
 										Integer pixelWidth, 
 										Integer pixelHeight, 
 										Integer initialPixelWidth, 
 										Integer initialPixelHeight) {
		StringBuilder result = new StringBuilder(128);
		result.append("/images/dynamic.png?").append(AbstractWebContext.DOCUMENT_NAME).append('=');
		result.append(moduleName).append('.').append(documentName);
		result.append('&').append(DynamicImageServlet.IMAGE_NAME).append('=').append(name);
		if (pixelWidth != null) {
			result.append('&').append(DynamicImageServlet.IMAGE_WIDTH_NAME).append('=').append(pixelWidth);
		}
		else if (initialPixelWidth != null) {
			result.append('&').append(DynamicImageServlet.IMAGE_WIDTH_NAME).append('=').append(initialPixelWidth);
		}
		else {
			result.append('&').append(DynamicImageServlet.IMAGE_WIDTH_NAME).append("=200");
		}
		if (pixelHeight != null) {
			result.append('&').append(DynamicImageServlet.IMAGE_HEIGHT_NAME).append('=').append(pixelHeight);
		}
		else if (initialPixelHeight != null) {
			result.append('&').append(DynamicImageServlet.IMAGE_HEIGHT_NAME).append('=').append(initialPixelHeight);
		}
		else {
			result.append('&').append(DynamicImageServlet.IMAGE_HEIGHT_NAME).append("=200");
		}
		result.append('&').append(DynamicImageServlet.IMAGE_WIDTH_ZOOM_NAME).append("=100");
		result.append('&').append(DynamicImageServlet.IMAGE_HEIGHT_ZOOM_NAME).append("=100");
		result.append('&').append(AbstractWebContext.CONTEXT_NAME).append('=').append(getWebContext().getWebId());
		result.append("&bizId=").append(getCurrentBean().getBean().getBizId());
		result.append("_ts=").append(System.currentTimeMillis());
		
		return result.toString();
 	}

	@SuppressWarnings("static-method")
	public List<SelectItem> getSelectItems(String moduleName,
											String documentName,
											String binding,
											boolean includeEmptyItem) {
		final String key = new StringBuilder(64).append(moduleName).append('.').append(documentName).append('.').append(binding).toString();
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.finest("BeanMapAdapter.getSelectItems - key = " + key);
		return new GetSelectItemsAction(moduleName, documentName, binding, includeEmptyItem).execute();
	}
	
 	public List<BeanMapAdapter<Bean>> complete(String query) {
		UIComponent currentComponent = UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
		Map<String, Object> attributes = currentComponent.getAttributes();
		String completeModule = (String) attributes.get("module");
		String completeQuery = (String) attributes.get("query");
		String displayBinding = (String) attributes.get("display");

		// Take a defensive copy of the parameters collection and add the query to the description binding
		@SuppressWarnings("unchecked")
		List<FilterParameter> parameters = (List<FilterParameter>) attributes.get("parameters");
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		else {
			parameters = new ArrayList<>(parameters);
		}
		
		// Add the query parameter if its defined
		String parameterValue = Util.processStringValue(query);
		if (parameterValue != null) {
			FilterParameterImpl displayParameter = new FilterParameterImpl();
			displayParameter.setName(displayBinding);
			displayParameter.setOperator(FilterOperator.like);
			displayParameter.setValue(parameterValue);
			parameters.add(displayParameter);
		}
		
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - COMPLETE = " + completeModule + "." + completeQuery + " : " + query);
		return getBeans(completeModule, completeQuery, parameters);
	}

	// Used to hydrate the state after dehydration in SkyvePhaseListener.afterRestoreView()
 	// NB This is only set when the bean is dehydrated
	private String dehydratedWebId;
	public String getDehydratedWebId() {
		return dehydratedWebId;
	}

 	// restore the webContext and current bean etc
	public void hydrate(AbstractWebContext newWebContext)
	throws Exception {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - hydrate");
		webContext = newWebContext;
		dehydratedWebId = null;
		setBean(getBean());
	}

	// remove the webContext and current bean etc leaving only the webId
	public void dehydrate() {
		if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - dehydrate");
		if (webContext != null) {
			dehydratedWebId = webContext.getWebId();
			if (UtilImpl.FACES_TRACE) UtilImpl.LOGGER.info("FacesView - dehydratedWebId=" + dehydratedWebId);
		}
		webContext = null;
		lazyDataModels.clear();
		dualListModels.clear();
		beans.clear();
		currentBean = null;
	}
	
	/**
	 * This method produces a style class that implements 
	 * the form column/row contract in the skyve view metadata.
	 * This method is called within the div styleClass attribute
	 * in form layouts.
	 * @param formIndex	The form to get the style for.
	 * @param colspan	The colspan to style for.
	 * @return	The responsive grid style classes required.
	 */
	@SuppressWarnings({"unchecked", "static-method"})
	public String getResponsiveFormStyle(int formIndex, String alignment, int colspan) {
		List<ResponsiveFormGrid> formStyles = (List<ResponsiveFormGrid>) FacesContext.getCurrentInstance().getViewRoot().getAttributes().get(FacesUtil.FORM_STYLES_KEY);
		String result = formStyles.get(formIndex).getStyle(colspan);
		if (alignment != null) {
			result = String.format("%s %s", result, alignment);
		}
		
		return result;
	}
	
	/**
	 * This method produces a style class for each edit view form row.
	 * The side-effect is that the style is reset for the new row to layout.
	 * This method is called within the div styleClass attribute
	 * in form layouts.
	 * @param formIndex	The form to reset the style for.
	 * @return ui-g-12 ui-g-nopad
	 */
	@SuppressWarnings({"unchecked", "static-method"})
	public String resetResponsiveFormStyle(int formIndex) {
		List<ResponsiveFormGrid> formStyles = (List<ResponsiveFormGrid>) FacesContext.getCurrentInstance().getViewRoot().getAttributes().get(FacesUtil.FORM_STYLES_KEY);
		formStyles.get(formIndex).reset();
		return "ui-g-12 ui-g-nopad";
	}
}
