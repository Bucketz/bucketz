package org.bucketz.impl;

import aQute.bnd.annotation.headers.ProvideCapability;

//Need to provide the capability for the resolver
@ProvideCapability(
   ns = "osgi.service",
   value = "objectClass:List<String>=\"org.osgi.service.component.ComponentFactory\"" )
public class ComponentFactoryResolution
{

}
