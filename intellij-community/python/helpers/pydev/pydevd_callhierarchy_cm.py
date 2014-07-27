__author__ = 'user'


def get_frame_info(frame): #return func_name, line_no, filename
    co = frame.f_code
    return co.co_name, frame.line_no, co.co_filename # or frame.f_globals["__file__"]


class CallHierarchyCacheManager(object): #stores for every function in file its own callers
    def __init__(self):
        self._cache = {}

    def print_cache(self):
        pass

    def add(self, frame): #result is True if it is not repetition otherwise return false
        caller_frame = frame.f_back
        if caller_frame is None:
            return False

        result = False

        func_name, func_def_line_no, func_filename = get_frame_info(frame)
        caller_func_name, func_call_line_no, caller_filename = get_frame_info(caller_frame)

        if not func_filename in self._cache:
            self._cache[func_filename] = {}
            result = True

        cache_for_file = self._cache[func_filename]

        if not func_name in cache_for_file:
            cache_for_file[func_name] = {}
            result = True

        cache_for_func = cache_for_file[func_name]

        if not caller_filename in cache_for_func:
            cache_for_func[caller_filename] = {}
            result = True

        cache_for_caller_file = cache_for_func[caller_filename]

        if not caller_func_name in cache_for_caller_file:
            cache_for_caller_file[caller_func_name] = {}
            result = True

        cache_for_func_caller = cache_for_caller_file[caller_func_name]

        if not func_call_line_no in cache_for_func_caller:
            cache_for_func_caller[func_call_line_no] = None
            result = True

        return result

    def is_repetition(self, frame):
        caller_frame = frame.f_back

        if caller_frame is None:
            return True

        func_name, func_def_line_no, func_filename = get_frame_info(frame)
        caller_func_name, func_call_line_no, caller_filename = get_frame_info(caller_frame)

        if func_filename in self._cache:
            cache_for_file = self._cache[func_filename]
            if func_name in cache_for_file:
                cache_for_func = cache_for_file[func_name]
                if caller_filename in cache_for_func:
                    cache_for_caller_file = cache_for_func[caller_filename]
                    if caller_func_name in cache_for_caller_file:
                        cache_for_func_caller = cache_for_caller_file[caller_func_name]
                        if func_call_line_no in cache_for_func_caller:
                            return True

        return False