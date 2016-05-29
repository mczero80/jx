#include "all.h"


/*
 * Credential Variable
 */
ClassDesc *credentialvariableClass;

void credentialvariable_set(CredentialProxy * self, ObjectDesc * value)
{
	if (curdom()->id != self->signerDomainID) {
		ObjectDesc *self = createExceptionInDomain(curdom(),
							   "java/lang/RuntimeException",
							   "ATTEMPT TO SET CREDENTIAL DATE IN WRONG DOMAIN");
		exceptionHandler(self);
	}
	self->value = value;
}

ObjectDesc *credentialvariable_get(CredentialProxy * self)
{
	return self->value;
}

jint credentialvariable_getSignerDomainID(CredentialProxy * self)
{
	return self->signerDomainID;
}

MethodInfoDesc credentialvariableMethods[] = {
	{"set", "", credentialvariable_set}
	,
	{"get", "", credentialvariable_get}
	,
	{"getSignerDomainID", "", credentialvariable_getSignerDomainID}
	,
};

static jbyte credentialvariableTypeMap[] = { 1 };

void init_credential_portal()
{
	credentialvariableClass =
	    init_zero_class("jx/zero/Credential", credentialvariableMethods, sizeof(credentialvariableMethods), 2,
			    credentialvariableTypeMap, "<jx/zero/Credential>");
}
