# encoding: utf-8
# module lxml.etree
# from /usr/lib/python2.7/dist-packages/lxml/etree.so
# by generator 1.135
"""
The ``lxml.etree`` module implements the extended ElementTree API
for XML.
"""

# imports
import __builtin__ as __builtins__ # <module '__builtin__' (built-in)>

from LxmlSyntaxError import LxmlSyntaxError

from XPathError import XPathError

class XPathSyntaxError(LxmlSyntaxError, XPathError):
    # no doc
    def __init__(self, *args, **kwargs): # real signature unknown
        pass

    __qualname__ = 'XPathSyntaxError'


