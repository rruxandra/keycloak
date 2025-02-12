package org.keycloak.admin.ui.rest.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

public class AuthenticationMapper {
    private static final int MAX_USED_BY = 9;

    public static Authentication convertToModel(AuthenticationFlowModel flow, RealmModel realm) {

        final Stream<IdentityProviderModel> identityProviders = realm.getIdentityProvidersStream();

        final Authentication authentication = new Authentication();
        authentication.setId(flow.getId());
        authentication.setAlias(flow.getAlias());
        authentication.setBuiltIn(flow.isBuiltIn());
        authentication.setDescription(flow.getDescription());

        final List<String> usedByIdp = identityProviders.filter(idp -> flow.getId().equals(idp.getFirstBrokerLoginFlowId())
                        || flow.getId().equals(idp.getPostBrokerLoginFlowId()))
                .map(IdentityProviderModel::getAlias).limit(MAX_USED_BY).collect(Collectors.toList());
        if (!usedByIdp.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.SPECIFIC_PROVIDERS, usedByIdp));
        }


        Stream<ClientModel> browserFlowOverridingClients = realm.searchClientByAuthenticationFlowBindingOverrides(Collections.singletonMap("browser", flow.getId()), 0, MAX_USED_BY);
        Stream<ClientModel> directGrantFlowOverridingClients = realm.searchClientByAuthenticationFlowBindingOverrides(Collections.singletonMap("direct_grant", flow.getId()), 0, MAX_USED_BY);
        final List<String> usedClients = Stream.concat(browserFlowOverridingClients, directGrantFlowOverridingClients)
                .limit(MAX_USED_BY)
                .map(ClientModel::getClientId).collect(Collectors.toList());

        if (!usedClients.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.SPECIFIC_CLIENTS, usedClients));
        }

        final List<String> useAsDefault = Stream.of(realm.getBrowserFlow(), realm.getRegistrationFlow(), realm.getDirectGrantFlow(),
                        realm.getResetCredentialsFlow(), realm.getClientAuthenticationFlow(), realm.getDockerAuthenticationFlow(), realm.getFirstBrokerLoginFlow())
                .filter(f -> flow.getAlias().equals(f.getAlias())).map(AuthenticationFlowModel::getAlias).collect(Collectors.toList());

        if (!useAsDefault.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.DEFAULT, useAsDefault));
        }

        return authentication;
    }
}
