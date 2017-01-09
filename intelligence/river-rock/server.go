package main

import (
    "log"
    "errors"
	//"net/http"
    "encoding/json"
    "net/http"
    "net/http/httputil"
    "net/url"
    "database/sql"
	"github.com/labstack/echo"  
    _ "github.com/lib/pq"
)

const (
	sqlGetRefs = "select refs from resource_map where cluster_id=$1 and res=$2 limit 1"
)

func getRefs(db* sql.DB, clusterId int, res string) (string, error) {

    var refs string

    stmt, err := db.Prepare(sqlGetRefs)
    if err != nil {
        return refs, err
    }

    row := stmt.QueryRow(clusterId, res)
    if err := row.Scan(&refs); err != nil {
        return refs, err
    }

    return refs, nil
}

func selectRefFromArray(clusterId int, refs []interface{}, encodedRefs string) (string, error) {
    return "", nil

}

func selectRef(clusterId int, encodedRefs string) (string, error) { 
    var refs interface{}
    refBytes := []byte(encodedRefs)
    if err := json.Unmarshal(refBytes, &refs); err != nil {
        log.Print(err)
        return "", err
    }

    switch refs.(type) {
        case string:  
            return refs.(string), nil
        case []interface{}: 
            return selectRefFromArray(clusterId, refs.([]interface{}), encodedRefs)
    }
    return "", errors.New("unable to map refs json to a valid type: " + encodedRefs) 
}

func main() {
   	db, err := sql.Open("postgres", "user=ic dbname=ic sslmode=disable")
    if err != nil {
       log.Fatal(err)
    }

    fallback, err := url.Parse("http://localhost:1323/raw")
    if err != nil {
       log.Fatal(err)
    }

	e := echo.New()

    e.GET("/v1/*", func(c echo.Context) error {
        req := c.Request()
        res := c.Response()

        path := req.URL.Path    
        clusterId := 1
        refs, err := getRefs(db, clusterId, path)

        proxy := httputil.NewSingleHostReverseProxy(fallback)

        if err != nil {
            log.Print(err)
            proxy.ServeHTTP(res, req)
        } else {
            log.Print("REFS: " + refs)

            ref, err := selectRef(clusterId, refs)
            if err != nil {
                log.Print(err)
            } else {
                req.URL.Path = ref
            }

            proxy.ServeHTTP(res, req)
        }

        return nil
	})

    e.GET("/raw/*", func(c echo.Context) error {
        req := c.Request()
        log.Print(req.URL.Path)
        return c.String(http.StatusOK, req.URL.Path)
    })

	e.Logger.Fatal(e.Start(":1323"))
}
