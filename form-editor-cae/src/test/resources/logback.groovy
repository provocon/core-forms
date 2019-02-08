/*
 *
 *  Conceived, developed, and written 2018 and later by
 *
 *    Provocon for Deutsche Telekom AG
 *
 *  and maintained afterwards. German law applicable.
 *
 */

// Log configuration for test setup
scan '3600 seconds'

def appenders = []
appender('CONSOLE', ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = '%-5level %logger{35}.%msg%n'
  }
}
appenders.add('CONSOLE')

root WARN, appenders
logger "com.tallence", DEBUG, appenders, false
