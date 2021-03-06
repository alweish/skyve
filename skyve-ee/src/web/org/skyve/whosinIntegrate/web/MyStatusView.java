
package org.skyve.whosinIntegrate.web;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import modules.admin.domain.Contact;
import modules.whosinIntegrate.domain.MyStatus;
import modules.whosinIntegrate.domain.Staff;

import org.apache.commons.codec.binary.Base64;
import org.skyve.CORE;
import org.skyve.EXT;
import org.skyve.content.AttachmentContent;
import org.skyve.content.ContentManager;
import org.skyve.content.MimeType;
import org.skyve.impl.web.faces.FacesAction;
import org.skyve.impl.web.faces.beans.FacesView;
import org.skyve.metadata.user.User;
import org.skyve.persistence.Persistence;
import org.skyve.util.Util;
import org.skyve.web.WebAction;

@ViewScoped
@ManagedBean
public class MyStatusView extends FacesView<MyStatus> {
	private static final long serialVersionUID = -6668236628273137985L;

	@Override
	public void preRender() {
		if (! FacesContext.getCurrentInstance().isPostback()) {
			setWebActionParameter(WebAction.e);
			setBizModuleParameter(MyStatus.MODULE_NAME);
			setBizDocumentParameter(MyStatus.DOCUMENT_NAME);
		}
		
		super.preRender();
	}

	private String base64Image = null;
	public String getBase64Image() {
		return base64Image;
	}
	public void setBase64Image(String base64Image) {
		this.base64Image = base64Image;
	}

	public void saveStaff() {
		new FacesAction<Void>() {
			@Override
			@SuppressWarnings("synthetic-access")
			public Void callback() throws Exception {
				FacesContext ctx = FacesContext.getCurrentInstance();
				Persistence p = CORE.getPersistence();
				User u = p.getUser();

				MyStatus bean = getBean();
				Staff staff = bean.getMyStaff();
				if (staff == null) {
					ctx.addMessage(null, 
									new FacesMessage(FacesMessage.SEVERITY_ERROR, 
														"Your user does not have a staff member assigned",
														null));
					return null;
				}
				
				if (base64Image != null) {
					// remove "data:image/png;base64," from the start
					int start = base64Image.indexOf(',') + 1;
					if (start > 0) {
						byte[] bytes = Base64.decodeBase64(base64Image.substring(start).getBytes());

						String bizCustomer = u.getCustomerName();
						Contact contact = staff.getContact();
						try (ContentManager cm = EXT.newContentManager()) {
							AttachmentContent content = new AttachmentContent(bizCustomer, 
																				Contact.MODULE_NAME, 
																				Contact.DOCUMENT_NAME,
																				u.getDataGroupId(), 
																				u.getId(), 
																				contact.getBizId(), 
																				Contact.imagePropertyName,
																				MimeType.png,
																				bytes);
							content.setContentId(contact.getImage());
							cm.put(content);
							contact.setImage(content.getContentId());
							changedContactImageId = contact.getImage(); // the next render of graphic image will drop the cache
							base64Image = null;
						}
					}
				}

				staff = p.save(staff);
				bean.setMyStaff(staff);
				
				return null;
			}
		}.execute();
	}
	
	// For refreshing images
	private String changedContactImageId;
	public boolean shouldCacheImage(String contactImageId) {
		String processedContactImageId = Util.processStringValue(contactImageId);
		if (processedContactImageId != null) {
			if (processedContactImageId.equals(changedContactImageId)) {
				changedContactImageId = null;
				return false;
			}
		}
		return true;
	}
}
