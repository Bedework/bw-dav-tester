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
        self.manager = manager
        self.callback = None
        self.args = {}

    public void doGenerate () {

        # Re-do substitutions from values generated during the current test run
        if self.manager.server_info.hasextrasubs() {
            for name, values in self.args.iteritems() {
                newvalues = [self.manager.server_info.extrasubs(value) for value in values]
                self.args[name] = newvalues

        generatorClass = self._importName(self.callback, "Generator")
        gen = generatorClass()

        # Always clone the args as this verifier may be called multiple times
        args = dict((k, list(v)) for k, v in self.args.items())

        return gen.generate(self.manager, args)

    public void _importName (modulename, name) {
        """
        Import a named object from a module in the context of this function.
        """
        module = __import__(modulename, globals(), locals(), [name])
        return getattr(module, name)

    public void parseXML (node) {

        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_CALLBACK:
                self.callback = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_ARG:
                self.parseArgXML(child)

    public void parseArgXML (node) {
        name = None
        values = []
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_NAME:
                name = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE:
                if child.text != null:
                    values.append(self.manager.server_info.subs(contentUtf8(child)))
                } else {
                    values.append("")
        if name:
            self.args[name] = values
