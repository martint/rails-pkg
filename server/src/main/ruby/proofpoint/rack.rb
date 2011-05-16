require 'rubygems'
require 'bundler/setup'
require 'rack'

module Proofpoint
  module Rack
    class Builder
      def build(filename, script, logger)
        rack_app = eval("Rack::Builder.new {(" + script + "\n )}.to_app", TOPLEVEL_BINDING, filename)
        ServletAdapter.new(rack_app, logger)
      end
    end

    class RackInput
      def initialize(input_stream)
        @input_stream = input_stream.to_io
      end

      def gets
        @input_stream.gets
      end

      def read(length, buffer = nil)
        @input_stream.read(length, buffer)
      end

      def each(&block)
        @input_stream.each(&block)
      end

      def rewind
        raise "rewind not yet supported"
      end

      def close
        raise "close should never be called on rack.input"
      end
    end

    class RackLogger
      def initialize(logger)
        @logger = logger
      end

      def debug(message)
        @logger.debug(message)
      end

      def info(message)
        @logger.info(message)
      end

      def warn(message)
        @logger.warn(message)
      end

      def error(message)
        @logger.error(message)
      end

      def fatal(message)
        @logger.error(message)
      end
    end

    class ServletAdapter
      def initialize(app, logger)
        @app = app
        @logger = RackLogger.new(logger)
        @errors = java::lang::System::err.to_io # TODO: write to logger
      end

      def call(servlet_request, servlet_response)
        env = {
                'rack.version' => ::Rack::VERSION,
                'rack.multithread' => true,
                'rack.multiprocess' => false,
                'rack.input' => RackInput.new(servlet_request.input_stream),
                'rack.errors' => @errors,
                'rack.logger' => @logger,
                'rack.url_scheme' => servlet_request.scheme,
                'REQUEST_METHOD' => servlet_request.method,
                'SCRIPT_NAME' => '',
                'PATH_INFO' => servlet_request.path_info,
                'QUERY_STRING' => servlet_request.query_string,
                'SERVER_NAME' => servlet_request.server_name,
                'SERVER_PORT' => servlet_request.server_port.to_s
        }

        env['CONTENT_TYPE'] = servlet_request.content_type unless servlet_request.content_type.nil?
        env['CONTENT_LENGTH']  = servlet_request.content_length unless servlet_request.content_length.nil?

        servlet_request.header_names.each do |header|
          next if header =~ /^Content-(Type|Length)$/i
          key = "HTTP_#{header.upcase.gsub(/-/, '_')}"
          env[key] = servlet_request.get_header(header)
        end

        status, headers, body = @app.call(env)

        servlet_response.set_status(status)
        headers.each do |key, value|
          case key
            when /^Content-Type$/i
              servlet_response.content_type = value.to_s
            when /^Content-Length$/i
              servlet_response.content_length = value.to_i
            else
              servlet_response.add_header(key.to_s, value.to_s)
          end
        end

        stream = servlet_response.output_stream
        body.each do |part|
          stream.write(part.to_java_bytes)
        end
        stream.flush
      end
    end
  end
end
