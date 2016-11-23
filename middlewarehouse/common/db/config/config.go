package config

import (
	"fmt"
	"log"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/common"
	"github.com/FoxComm/highlander/middlewarehouse/common/db"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/common/logging"
	"github.com/jinzhu/gorm"
	_ "github.com/lib/pq" // Needed by gorm.
)

var (
	// defaultConnection stores the gorm.DB (which holds the underlying database/sql.DB)
	// for reuse amongst multiple components/goroutines and is threadsafe as both gorm.DB
	// and database/sql.DB are threadsafe.
	defaultConnection *gorm.DB
)

// PGConfig is the set of configuration options needed to connect to Postgres.
type PGConfig struct {
	User         string
	Password     string
	DatabaseName string
	Host         string
	SSLMode      string
}

func NewPGConfig() *PGConfig {
	dbUser := os.Getenv("DB_USER")
	dbPassword := os.Getenv("DB_PASSWORD")
	dbName := os.Getenv("DB_NAME")
	dbHost := os.Getenv("DB_HOST")
	dbSSLMode := os.Getenv("DB_SSLMODE")

	return &PGConfig{
		User:         dbUser,
		Password:     dbPassword,
		DatabaseName: dbName,
		Host:         dbHost,
		SSLMode:      dbSSLMode,
	}
}

// Connect initializes the connection with Postgres based on a configuration.
func Connect(config *PGConfig) (*gorm.DB, exceptions.IException) {
	conn := fmt.Sprintf("dbname=%s sslmode=%s", config.DatabaseName, config.SSLMode)

	if config.User != "" {
		conn = fmt.Sprintf("user=%s %s", config.User, conn)
	}
	if config.Password != "" {
		conn = fmt.Sprintf("password=%s %s", config.Password, conn)
	}
	if config.Host != "" {
		conn = fmt.Sprintf("host=%s %s", config.Host, conn)
	}

	database, err := gorm.Open("postgres", conn)
	return database, db.NewDatabaseException(err)
}

// DefaultConnection returns the defaultConnection var if it's been set; otherwise, it
// calls Connect() resulting in a new *gorm.DB which is then held in defaultConnection for
// reuse.
//
// We reuse the connection because if we didn't we could end up calling gorm.Open() until
// we hit the PostgreSQL connection limit. This occurs because each call to gorm.Open() results
// in a new gorm.DB which creates at least one new connection to the DB. Instead, we reuse
// which means the underlying database/sql.DB can pool connections as necessary. This means
// our use of the DB is more efficient and avoids hitting connection limit errors.
//
// If, in the future, we connect to multiple DSNs, we should change
// defaultConnection to a map[string]*gorm.DB allowing us to continue to reuse the underlying
// database/sql.DB connection(s) for each DSN.
//
// Read here for more information:
//  - http://go-database-sql.org/connection-pool.html
//  - https://github.com/jinzhu/gorm/blob/ef4299b39879ad31b5511acecc12ef4457276d40/main.go#L39-L79
//  - https://github.com/golang/go/blob/master/src/database/sql/sql.go#L200-L211
func DefaultConnection() (*gorm.DB, exceptions.IException) {
	var exception exceptions.IException

	if defaultConnection == nil {
		defaultConnection, exception = Connect(NewPGConfig())
		defaultConnection.SetLogger(logging.NewGormLogger(logging.Log))
	}

	return defaultConnection, exception
}

func TestConnection() *gorm.DB {
	db, exception := Connect(NewPGConfig())

	if exception != nil {
		log.Panicf("Failed to connect to test db with %s", exception.ToString())
	}

	return db
}

func init() {
	common.MustLoadEnv()
}
