#include "util/dbc.hpp"

#include <cstdlib>
#include <execinfo.h>

namespace bernardo 
{ 
    namespace util
    {
        namespace 
        {
            const size_t TRACE_SIZE = 16;
        }

        void trace(std::ostream& o)
        {
            void *t[TRACE_SIZE];
            auto size = backtrace(t, TRACE_SIZE);
            auto s = backtrace_symbols (t, size);
            for (int i = 0; i < size; i++)
                o << s[i] << std::endl;

            std::free(s);
        }

        void raise(const char * msg) 
        {
            std::stringstream s;
            s << msg << std::endl;
            trace(s);
            std::cerr << s.str() << std::endl;
            exit(1);
        }

        void raise1( 
                const char * file,
                const char * func,
                const int line,
                const char * dbc,
                const char * expr)
        {
            std::stringstream s;
            s << "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" << std::endl;
            s << "!! " << dbc << " failed" << std::endl;
            s << "!! expr: " << expr << std::endl;
            s << "!! func: " << func << std::endl;
            s << "!! file: " << file << " (" << line << ")" << std::endl;
            s << "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" << std::endl;
            raise(s.str().c_str());
        }
    }
}
