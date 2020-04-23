/**
 * Allow for some logging during tests
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
