/*
# Copyright (c) 2006-2016 Apple Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package org.bedework.davtester.request;

class Generator{
    """
    Defines a dynamically generated request body.
    """

    public Generator(manager) {
        manager = manager
        callback = null
        args = {}

    public void doGenerate () {

        # Re-do substitutions from values generated during the current test run
        if manager.serverInfo.hasextrasubs() {
            for name, values in args.iteritems() {
                newvalues = [manager.serverInfo.extrasubs(value) for value in values]
                args[name] = newvalues

        generatorClass = _importName(callback, "Generator")
        gen = generatorClass()

        # Always clone the args as this verifier may be called multiple times
        args = dict((k, list(v)) for k, v in args.items())

        return gen.generate(manager, args)

    public void _importName (modulename, name) {
        """
        Import a named object from a module in the context of this function.
        """
        module = __import__(modulename, globals(), locals(), [name])
        return getattr(module, name)

    public void parseXML (node) {

        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_CALLBACK:
                callback = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_ARG:
                parseArgXML(child)

    public void parseArgXML (node) {
        name = null
        values = []
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_NAME:
                name = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE:
                if child.text != null:
                    values.append(manager.serverInfo.subs(contentUtf8(child)))
                } else {
                    values.append("")
        if name:
            args[name] = values
