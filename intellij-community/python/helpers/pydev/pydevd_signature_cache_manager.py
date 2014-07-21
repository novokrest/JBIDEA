__author__ = 'user'

import pydevd_vars
import sys


def get_signature_info(signature):
    return signature.file, signature.name, ' '.join([arg[1]for arg in signature.args])


class SignatureCacheManager(object):
    def __init__(self, log=None):
        self.cache = {}
        self.log = log

    def get_cache_size(self):
        return sys.getsizeof(self.cache)

    #only for analysis
    def write_cache_size(self, default=None):
        cache_size = self.get_cache_size()
        if self.log:
            open(self.log, 'w').write(str(cache_size))
            #self.log.write(str(cache_size) + '\n')


class CallSignatureCacheManager(SignatureCacheManager):
    def __init__(self, log=None):
        SignatureCacheManager.__init__(self, log)

    def add(self, signature):
        filename, name, args_type = get_signature_info(signature)

        if not filename in self.cache:
            self.cache[filename] = {}

        calls_from_file = self.cache[filename]

        if not name in calls_from_file:
            calls_from_file[name] = {}

        name_calls = calls_from_file[name]
        name_calls[args_type] = None

        #self.write_cache_size()

    def is_repetition(self, signature):
        filename, name, args_type = get_signature_info(signature)
        if filename in self.cache and name in self.cache[filename] and args_type in self.cache[filename][name]:
            return True
        return False

    def is_first_call(self, signature):
        filename, name = get_signature_info(signature)[:-1]
        if filename in self.cache and name in self.cache[filename]:
            return False
        return True

    def print_cache(self):
        for filename, calls_from_file in self.cache.items():
            for name, args_type in calls_from_file.items():
                print "filename=%s, name=%s, args_type=%s" % (filename, name, args_type)


class ReturnSignatureCacheManager(SignatureCacheManager):
    def __init__(self, log=None):
        SignatureCacheManager.__init__(self, log)

    def add(self, signature, return_info):
        filename, name = get_signature_info(signature)[:-1]

        if not filename in self.cache:
            self.cache[filename] = {}

        calls_from_file = self.cache[filename]

        if not name in calls_from_file:
            calls_from_file[name] = {}

        returns = calls_from_file[name]
        returns[return_info] = None

        #self.write_cache_size()

    def is_repetition(self, signature, return_info):
        filename, name = get_signature_info(signature)[:-1]
        if filename in self.cache and name in self.cache[filename] and return_info in self.cache[filename][name]:
            return True
        return False

    def print_cache(self):
        for filename, calls_from_file in self.cache.items():
            for name, returns in calls_from_file.items():
                print 'filename=%s, name=%s, returns=%s' % (filename, name, returns)