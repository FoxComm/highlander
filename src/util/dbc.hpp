#ifndef ISAAC_DBC_H
#define ISAAC_DBC_H

#undef CHECK

#include <iostream>
#include <sstream>
#include <functional>

namespace isaac 
{
    namespace util 
    {
        using dialog_callback = std::function<void(const char*)>;
        void set_assert_dialog_callback(dialog_callback);

        void raise(const char * msg);
        void raise1( 
                const char * file, 
                const char * func, 
                const int, 
                const char * dbc, 
                const char * expr);

        template <typename T1, typename T2>
            void raise2(
                    const char * file, 
                    const char * func, 
                    const int line, 
                    const char * dbc, 
                    const char * expr, 
                    const char* n1, const T1& v1,
                    const char* n2, const T2& v2)
            {
                std::stringstream s;
                s << "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" << std::endl;
                s << "!! " << dbc << " failed" << std::endl;
                s << "!! expr: " << expr 
                    << " [" << n1 << " = " << v1 
                    << ", " << n2 << " = " << v2 << "]" << std::endl;
                s << "!! func: " << func << std::endl;
                s << "!! file: " << file << " (" << line << ")" << std::endl;
                s << "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" << std::endl;
                raise(s.str().c_str());
            }

        template <typename T1, typename T2, typename T3>
            void raise3(
                    const char * file, 
                    const char * func, 
                    const int line, 
                    const char * dbc, 
                    const char * expr, 
                    const char* n1, const T1& v1,
                    const char* n2, const T2& v2,
                    const char* n3, const T3& v3)
            {
                std::stringstream s;
                s << "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" << std::endl;
                s << "!! " << dbc << " failed" << std::endl;
                s << "!! expr: " << expr 
                    << " [" << n1 << " = " << v1 
                    << ", " << n2 << " = " << v2 
                    << ", " << n3 << " = " << v3 << "]" << std::endl;
                s << "!! func: " << func << std::endl;
                s << "!! file: " << file << " (" << line << ")" << std::endl;
                s << "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" << std::endl;
                raise(s.str().c_str());
            }
    }
}


#define F_STR(E) #E
#define F_R1(dbc, exp) if(!(exp)) { isaac::util::raise1( __FILE__, __FUNCTION__, __LINE__, dbc, F_STR(exp)); } 
#define F_R2(dbc, exp, a, b) if(!(exp)) { isaac::util::raise2( __FILE__, __FUNCTION__, __LINE__, dbc, F_STR(exp), F_STR(a), a, F_STR(b), b); } 
#define F_R3(dbc, exp, a, b, c) if(!(exp)) { isaac::util::raise3( __FILE__, __FUNCTION__, __LINE__, dbc, F_STR(exp), F_STR(a), a, F_STR(b), b, F_STR(c), c); } 

#define F_C "check"
#define F_R "require"
#define F_E "ensure"
#define F_I "invariant"

#define R_CHECK(dbc, exp) F_R1(dbc, exp)
#define CHECK(exp) R_CHECK(F_C, exp)
#define REQUIRE(exp) R_CHECK(F_R, exp)
#define ENSURE(exp) R_CHECK(F_E, exp)
#define INVARIANT(exp) R_CHECK(F_I, exp)

#define R_CHECK_FALSE(dbc, exp) F_R1(dbc, (!(exp)))
#define CHECK_FALSE(exp) R_CHECK_FALSE(F_C, exp)
#define REQUIRE_FALSE(exp) R_CHECK_FALSE(F_R, exp)
#define ENSURE_FALSE(exp) R_CHECK_FALSE(F_E, exp)
#define INVARIANT_FALSE(exp) R_CHECK_FALSE(F_I, exp)

#define R_EQUAL(dbc, a, b) F_R2(dbc, (a == b), a, b)
#define CHECK_EQUAL(a, b) R_EQUAL(F_C, a, b)
#define REQUIRE_EQUAL(a, b) R_EQUAL(F_R, a, b)
#define ENSURE_EQUAL(a, b) R_EQUAL(F_E, a, b)
#define INVARIANT_EQUAL(a, b) R_EQUAL(F_I, a, b)

#define R_NOT_EQUAL(dbc, a, b) F_R2(dbc, (a != b), a, b)
#define CHECK_NOT_EQUAL(a, b) R_NOT_EQUAL(F_C, a, b)
#define REQUIRE_NOT_EQUAL(a, b) R_NOT_EQUAL(F_R, a, b)
#define ENSURE_NOT_EQUAL(a, b) R_NOT_EQUAL(F_E, a, b)
#define INVARIANT_NOT_EQUAL(a, b) R_NOT_EQUAL(F_I, a, b)

#define R_LESS(dbc, a, b) F_R2(dbc, (a < b), a, b)
#define CHECK_LESS(a, b) R_LESS(F_C, a, b)
#define REQUIRE_LESS(a, b) R_LESS(F_R, a, b)
#define ENSURE_LESS(a, b) R_LESS(F_E, a, b)
#define INVARIANT_LESS(a, b) R_LESS(F_I, a, b)

#define R_LESS_EQUAL(dbc, a, b) F_R2(dbc, (a <= b), a, b)
#define CHECK_LESS_EQUAL(a, b) R_LESS_EQUAL(F_C, a, b)
#define REQUIRE_LESS_EQUAL(a, b) R_LESS_EQUAL(F_R, a, b)
#define ENSURE_LESS_EQUAL(a, b) R_LESS_EQUAL(F_E, a, b)
#define INVARIANT_LESS_EQUAL(a, b) R_LESS_EQUAL(F_I, a, b)

#define R_GREATER(dbc, a, b) F_R2(dbc, (a > b), a, b)
#define CHECK_GREATER(a, b) R_GREATER(F_C, a, b)
#define REQUIRE_GREATER(a, b) R_GREATER(F_R, a, b)
#define ENSURE_GREATER(a, b) R_GREATER(F_E, a, b)
#define INVARIANT_GREATER(a, b) R_GREATER(F_I, a, b)

#define R_GREATER_EQUAL(dbc, a, b) F_R2(dbc, (a >= b), a, b)
#define CHECK_GREATER_EQUAL(a, b) R_GREATER_EQUAL(F_C, a, b)
#define REQUIRE_GREATER_EQUAL(a, b) R_GREATER_EQUAL(F_R, a, b)
#define ENSURE_GREATER_EQUAL(a, b) R_GREATER_EQUAL(F_E, a, b)
#define INVARIANT_GREATER_EQUAL(a, b) R_GREATER_EQUAL(F_I, a, b)

#define R_RANGE(dbc, a, b, c) F_R3(dbc, (a >= b && a < c), a, b, c)
#define CHECK_RANGE(a, b, c) R_RANGE(F_C, a, b, c)
#define REQUIRE_RANGE(a, b, c) R_RANGE(F_R, a, b, c)
#define ENSURE_RANGE(a, b, c) R_RANGE(F_E, a, b, c)
#define INVARIANT_RANGE(a, b, c) R_RANGE(F_I, a, b, c)

#define R_BETWEEN(dbc, a, b, c) F_R3(dbc, (a >= b && a <= c), a, b, c)
#define CHECK_BETWEEN(a, b, c) R_BETWEEN(F_C, a, b, c)
#define REQUIRE_BETWEEN(a, b, c) R_BETWEEN(F_R, a, b, c)
#define ENSURE_BETWEEN(a, b, c) R_BETWEEN(F_E, a, b, c)
#define INVARIANT_BETWEEN(a, b, c) R_BETWEEN(F_I, a, b, c)

#endif
