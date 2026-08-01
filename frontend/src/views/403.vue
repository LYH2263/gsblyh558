<template>
  <div class="container">
    <div class="row justify-content-center w-100">
      <div class="col-md-8 text-center">
        <h1 class="display-1 fw-bold text-danger">403</h1>
        <h2 class="mb-4">禁止访问</h2>
        <p class="lead mb-4">
          抱歉，您没有权限访问此页面或您的登录会话已过期。
          <br>
          请重新登录以继续。
        </p>
        <router-link to="/login" class="btn btn-primary btn-lg">
          <i class="bi bi-box-arrow-in-right"></i> 去登录
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.container {
  min-height: calc(100vh - 100px);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
