# Nginx serves the static files produced by "npm run build"
FROM nginx:stable-alpine

# Replaces Nginx's default site with ours (React Router support)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# The dist folder is built before (locally or in the pipeline) with: npm run build
COPY dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]