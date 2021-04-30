package org.comroid.webkit.oauth.rest.request;

import org.comroid.mutatio.model.Ref;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.webkit.oauth.OAuth;
import org.jetbrains.annotations.Nullable;

public class TokenRevocationRequest extends DataContainerBase<TokenRevocationRequest> {
    @RootBind
    public static final GroupBind<TokenRevocationRequest> Type
            = new GroupBind<>(OAuth.CONTEXT, "token-revocation-request");
    public static final VarBind<TokenRevocationRequest, String, String, String> TOKEN
            = Type.createBind("token")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<TokenRevocationRequest, String, String, String> TOKEN_HINT
            = Type.createBind("token_type_hint")
            .extractAs(StandardValueType.STRING)
            .build();
    public final Ref<String> token = getComputedReference(TOKEN);
    public final Ref<String> tokenHint = getComputedReference(TOKEN_HINT);

    public String getToken() {
        return token.assertion("token");
    }

    public @Nullable String getTokenHint() {
        return tokenHint.get();
    }

    public TokenRevocationRequest(Context context, UniNode body) {
        super(context, body.asObjectNode());
    }
}
