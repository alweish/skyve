package org.skyve.admin.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import modules.admin.Communication.CommunicationBizlet;
import modules.admin.Subscription.SubscriptionBizlet;

import org.skyve.CORE;
import org.skyve.impl.metadata.user.UserImpl;
import org.skyve.impl.web.faces.FacesAction;
import org.skyve.impl.web.faces.beans.Harness;
import org.skyve.metadata.customer.Customer;
import org.skyve.persistence.Persistence;

@ManagedBean
@ViewScoped
public class Unsubscribe extends Harness {
	private static final long serialVersionUID = 6713621260342289323L;

	// indicates if the RSVP processing on HTTP GET was successful or not
	private boolean success = false;

	public boolean isSuccess() {
		return success;
	}

	public void preRender() {
		new FacesAction<Void>() {
			@Override
			@SuppressWarnings("synthetic-access")
			public Void callback() throws Exception {
				FacesContext fc = FacesContext.getCurrentInstance();
				if (! fc.isPostback()) {
					Persistence p = CORE.getPersistence();
					UserImpl internalUser = (UserImpl) p.getUser();
					if (internalUser != null) { // NB not necessarily logged in
						Customer customer = internalUser.getCustomer();
						initialise(customer, internalUser, fc.getExternalContext().getRequestLocale());
					}
					
					String bizCustomer = fc.getExternalContext().getRequestParameterMap().get("c");
					String communicationId = fc.getExternalContext().getRequestParameterMap().get("i");
					String receiverIdentifier = fc.getExternalContext().getRequestParameterMap().get("r");

					boolean communicationExists = CommunicationBizlet.anonymouslyCommunicationExists(p, bizCustomer, communicationId);
					if (communicationExists) {

						boolean subscriptionExists = SubscriptionBizlet.anonymouslySubscriptionExists(p, bizCustomer, communicationId, receiverIdentifier);
						if (!subscriptionExists) {
							SubscriptionBizlet.anonymouslyUnsubscribe(p, bizCustomer, communicationId, receiverIdentifier);
						}

						// return true if the communication checks out - as this
						// is a legitimate request
						success = true;
					}
				}

				return null;
			}
		}.execute();
	}
}
